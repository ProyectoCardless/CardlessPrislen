package com.banco.CajerosCardless.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    // Twilio credentials
    private static final String ACCOUNT_SID = "AC9671f57b1a8e604a050087aaa91f50d1";
    private static final String AUTH_TOKEN = "129ede3f3d3d6e94c63d68fb05571fb1";
    private static final String FROM_PHONE_NUMBER = "+18705681441";

    public SmsService() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public void sendSms(String toPhoneNumber, String messageContent) {
        Message message = Message.creator(
                new PhoneNumber("+"+toPhoneNumber),
                new PhoneNumber(FROM_PHONE_NUMBER),
                messageContent).create();
    }
}
