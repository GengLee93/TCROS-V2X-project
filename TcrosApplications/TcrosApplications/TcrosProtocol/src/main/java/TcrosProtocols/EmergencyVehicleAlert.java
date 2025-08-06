package TcrosProtocols;

import CommonClass.EvaClass.Details;
import CommonEnum.BasicType;
import CommonEnum.ResponseType;
import jdk.jfr.Description;

import java.io.Serializable;

public record EmergencyVehicleAlert(
        @Description("緊急車輛對時時間點（MinuteOfTheYear），單位為分鐘，取值範圍 0–527040。")
        Integer timeStamp,

        @Description("車輛暫時編號（OCTET STRING, SIZE=4），用於識別發布訊息之車輛。")
        String id,

        @Description("路側告警訊息內容（RSA），包含事件位置、型態、信心度與事件時間等資訊。")
        RoadSideAlert rsaMsg,

        @Description("出勤型態，如：緊急（emergency）、非緊急（nonEmergency）、追趕（pursuit）等。")
        ResponseType responseType,

        @Description("緊急出勤細節")
        Details details,

        @Description("車重資訊，值域 0–255。不同區間對應不同單位，最大可表示超過 170,000kg。")
        Integer mass,

        @Description("車輛類型，例：小客車、摩托車、公車、多軸車輛等。")
        BasicType basicType
) implements Serializable {}
