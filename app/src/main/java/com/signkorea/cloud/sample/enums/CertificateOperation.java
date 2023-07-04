package com.signkorea.cloud.sample.enums;

public enum CertificateOperation {
    register,
    get,
    delete,
    export,
    changePin,
    issue,
    updateCloud,
    updateLocal,
    unlock;

    public String getLabel() {
        switch (this) {
            case register:
                return "인증서 보관";
            case delete:
                return "인증서 삭제";
            case export:
                return "인증서 내려받기";
            case changePin:
                return "인증서 PIN 변경";
            case issue:
                return "인증서 발급";
            case updateLocal:
                return "로컬 인증서 갱신";
            case updateCloud:
                return "클라우드 인증서 갱신";
            case unlock:
                return "인증서 잠금 해제";
            default:
                return "전자서명";
        }
    }
}
