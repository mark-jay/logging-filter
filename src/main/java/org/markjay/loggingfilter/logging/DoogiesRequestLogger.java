package org.markjay.loggingfilter.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Was stolen from here:
 * https://stackoverflow.com/a/42023374
 * https://stackoverflow.com/questions/33744875/spring-boot-how-to-log-all-requests-and-responses-with-exceptions-in-single-pl
 *
 * Doogies very cool HTTP request logging
 *
 * There is also {@link org.springframework.web.filter.CommonsRequestLoggingFilter}  but it cannot log request method
 * And it cannot easily be extended.
 *
 * https://mdeinum.wordpress.com/2015/07/01/spring-framework-hidden-gems/
 * http://stackoverflow.com/questions/8933054/how-to-read-and-copy-the-http-servlet-response-output-stream-content-for-logging
 */
@Component
@Order(1)
public class DoogiesRequestLogger extends OncePerRequestFilter {

    private boolean includeResponsePayload = true;
    private int maxPayloadLength = 1000;

    private static final List<String> ignoredHeaders = new ArrayList<String>() {{
        add("Authorization");
    }};

    private String getContentAsString(byte[] buf, int maxLength, String charsetName) {
        if (buf == null || buf.length == 0) return "";
        int length = Math.min(buf.length, maxLength);
        try {
            return new String(buf, 0, length, charsetName);
        } catch (UnsupportedEncodingException ex) {
            return "Unsupported Encoding";
        }
    }

    public static class LogModel {

        private String method;
        private String url;
        private Map<String, String> headers;
        private String request;
        private int httpStatus;
        private String response;
        private long durationMs;

        public LogModel(String url, Map<String, String> headers, String method) {
            this.url = url;
            this.headers = headers;
            this.method = method;
        }

        public static LogModel fromRequest(HttpServletRequest request) {
            return new LogModel(buildUrl(request), buildHeaders(request), request.getMethod());
        }

        private static Map<String, String> buildHeaders(HttpServletRequest request) {
            HashMap<String, String> res = new HashMap<>();

            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                res.put(headerName, request.getHeader(headerName));
            }

            return res;
        }

        private static String buildUrl(HttpServletRequest request) {
            StringBuilder reqInfo = new StringBuilder()
                    .append(request.getRequestURL());

            String queryString = request.getQueryString();
            if (queryString != null) {
                reqInfo.append("?").append(queryString);
            }

            if (request.getAuthType() != null) {
                reqInfo.append(", authType=")
                        .append(request.getAuthType());
            }
            if (request.getUserPrincipal() != null) {
                reqInfo.append(", principalName=")
                        .append(request.getUserPrincipal().getName());
            }
            return reqInfo.toString();
        }

        public String filterQuotes(String string) {
            if (string.contains("'")) {
                return string.replaceAll("'", "'\"'\"'");
            } else {
                return string;
            }
        }

        public String formatToCurl() {
            StringBuilder curl = new StringBuilder();
            curl.append("curl -v -X").append(method);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase("content-length")) {
                    curl.append(" -H '")
                            .append(filterQuotes(entry.getKey())).append(": ")
                            .append(filterQuotes(entry.getValue())).append("'");
                }
            }

            if (method.equalsIgnoreCase("post") && request !=null && !request.isEmpty()) {
                curl.append(" -d'").append(filterQuotes(request)).append("'");
            }

            curl.append(" '").append(url).append("'");
            return curl.toString();
        }

        public void filterHeaders() {
            for (String headerName : headers.keySet()) {
                if (ignoredHeaders.stream().anyMatch(ignoredHeader -> ignoredHeader.equalsIgnoreCase(headerName))) {
                    headers.put(headerName, "XXX");
                }
            }
        }

        public String formatToJson() throws JsonProcessingException {
            return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(this);
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public void setHttpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }

    /**
     * Log each request and respponse with full Request URI, content payload and duration of the request in ms.
     * @param request the request
     * @param response the response
     * @param filterChain chain of filters
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        LogModel logModel = LogModel.fromRequest(request);

        this.logger.info(String.format("=> %s %s", logModel.method, logModel.url));

        // ========= Log request and response payload ("body") ========
        // We CANNOT simply read the request payload here, because then the InputStream would be consumed and cannot be read again by the actual processing/server.
        //    String reqBody = DoogiesUtil._stream2String(request.getInputStream());   // THIS WOULD NOT WORK!
        // So we need to apply some stronger magic here :-)
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(wrappedRequest, wrappedResponse);     // ======== This performs the actual request!
        long duration = System.currentTimeMillis() - startTime;

        // I can only log the request's body AFTER the request has been made and ContentCachingRequestWrapper did its work.
        String requestBody = getContentAsString(wrappedRequest.getContentAsByteArray(), this.maxPayloadLength, request.getCharacterEncoding());
        if (requestBody.length() > 0) {
            logModel.request = requestBody;
        }

        logModel.httpStatus = response.getStatus();
        logModel.durationMs = duration;
        if (includeResponsePayload) {
            byte[] buf = wrappedResponse.getContentAsByteArray();
            String responseAsString = getContentAsString(buf, this.maxPayloadLength, response.getCharacterEncoding());
            logModel.response = responseAsString;
        }

        // IMPORTANT: copy content of response back into original response
        wrappedResponse.copyBodyToResponse();

        if (logger.isTraceEnabled()) {
            try {
                logger.trace(logModel.formatToCurl());
            } catch (Exception e) {
                logger.warn("Could not log curl request");
            }
        }

        logModel.filterHeaders();
        if (logger.isInfoEnabled()) {
            try {
                logger.info(logModel.formatToJson());
            } catch (JsonProcessingException e) {
                logger.warn("Could not log request and response as json");
            }
        }
    }


}