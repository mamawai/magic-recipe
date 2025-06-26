//package com.mawai.mrcrawler.model;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.Objects;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class Proxy {
//    private String host;
//    private int port;
//    private String fullAddress;
//
//    public String getFullAddress() {
//        return Objects.requireNonNullElseGet(fullAddress, () -> host + ":" + port);
//    }
//
//    public static Proxy fromString(String proxyString) {
//        String[] parts = proxyString.split(":");
//        if (parts.length < 2) {
//            return null;
//        }
//
//        return Proxy.builder()
//                .host(parts[0])
//                .port(Integer.parseInt(parts[1]))
//                .fullAddress(proxyString)
//                .build();
//    }
//}