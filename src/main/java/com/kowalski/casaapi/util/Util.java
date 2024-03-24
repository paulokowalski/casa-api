package com.kowalski.casaapi.util;

import java.text.Normalizer;

public class Util {

    public static String removerAcentos(String valor){
        String normalized = Normalizer.normalize(valor, Normalizer.Form.NFD);
        return normalized.replaceAll("[^\\p{ASCII}]", "");
    }
}
