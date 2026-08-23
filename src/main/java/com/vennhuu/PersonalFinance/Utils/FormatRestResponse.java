package com.vennhuu.PersonalFinance.Utils;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.vennhuu.PersonalFinance.Entity.Response.RestResponse;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public @Nullable Object beforeBodyWrite(
            @Nullable Object arg0, 
            MethodParameter arg1, 
            MediaType arg2,
            Class<? extends HttpMessageConverter<?>> arg3, 
            ServerHttpRequest arg4, 
            ServerHttpResponse arg5) {
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) arg5).getServletResponse();
        int status = servletResponse.getStatus();

        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatus(status);

        if (arg0 instanceof String || arg0 instanceof Resource) {
            return arg0;
        }

        String path = arg4.getURI().getPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return arg0;
        }

        if (status >= 400) {
            return arg0;
        } else {
            res.setData(arg0);
            APIMessage message = arg1.getMethodAnnotation(APIMessage.class);
            res.setMessage(message != null ? message.value() : "CALL API SUCCESS");
        }

        return res;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }
    
}
