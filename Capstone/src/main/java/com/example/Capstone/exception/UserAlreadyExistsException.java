package com.example.Capstone.exception;

import com.example.Capstone.entity.User;

public class UserAlreadyExistsException  extends RuntimeException{

    public UserAlreadyExistsException(String message){
        super(message);

    }


}
