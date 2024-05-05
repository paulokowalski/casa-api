package com.kowalski.casaapi.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class RecarregaEvent implements Serializable {

    public boolean recarrega;
}
