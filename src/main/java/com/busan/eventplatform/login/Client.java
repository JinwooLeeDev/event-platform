package com.busan.eventplatform.login;

// 저장된 session이 User인지 Company인지 구분해주기 위해.
public class Client {
    private Long id;
    private ClientType clientType;

    public enum ClientType {USER , COMPANY}

}
