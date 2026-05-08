package com.myapp.controller.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.NoSuchElementException;

@ControllerAdvice
public class WebSocketExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketExceptionHandler.class);

    public record ErrorMessage(String type, String msg){}

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleInvalidInputException (MethodArgumentNotValidException e){

        String msg = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        return new ErrorMessage("INVALID_INPUT", msg);
    }

    @MessageExceptionHandler(NoSuchElementException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleNotFoundException (NoSuchElementException e){

        return new ErrorMessage("NOT_FOUND", e.getMessage());
    }

    @MessageExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    @SendToUser("/queue/errors")
    public ErrorMessage handleLogicException (Exception e){

        return new ErrorMessage("LOGIC_ERROR", e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleGenericException (Exception e){

        log.error("Unhandled server exception in WebSocket:", e);
        return new ErrorMessage("SERVER_ERROR", e.getMessage());
    }

}
