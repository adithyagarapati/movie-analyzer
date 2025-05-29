package com.movieanalyzer.backend.config; // Or your main package if not using sub-packages for config

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure this filter is applied very early in the chain
public class SimpleCorsFilter implements Filter {

    public SimpleCorsFilter() {
        // Constructor
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        response.setHeader("Access-Control-Allow-Origin", "*"); // Allow all origins
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        response.setHeader("Access-Control-Max-Age", "3600"); // How long the results of a preflight request can be cached
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, observe"); // Include common headers and 'observe' if Axios uses it for progress
        // If you need to allow credentials (cookies, basic auth), set allowCredentials to "true"
        // and be more specific with Access-Control-Allow-Origin instead of "*"
        // response.setHeader("Access-Control-Allow-Credentials", "true");


        // For OPTIONS preflight requests, we need to send a 200 OK immediately
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Initialization code, if any
    }

    @Override
    public void destroy() {
        // Cleanup code, if any
    }
}