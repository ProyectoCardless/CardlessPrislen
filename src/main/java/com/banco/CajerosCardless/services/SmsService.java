package com.banco.CajerosCardless.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    // Twilio credentials
    private static final String ACCOUNT_SID = "ACf93687e609e287b73e11ab67940f3d54";
    private static final String AUTH_TOKEN = "1cedaecc48c478365a9ee441fa7cef07";
    private static final String FROM_PHONE_NUMBER = "+18705681441";

    public SmsService() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public void sendSms(String toPhoneNumber, String messageContent) {
        System.out.println(toPhoneNumber);
        Message message = Message.creator(
                new PhoneNumber("+"+toPhoneNumber),
                new PhoneNumber(FROM_PHONE_NUMBER),
                messageContent).create();
    }
}
