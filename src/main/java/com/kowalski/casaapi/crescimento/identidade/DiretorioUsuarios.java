package com.kowalski.casaapi.crescimento.identidade;

import java.util.Collection;
import java.util.Map;

public interface DiretorioUsuarios {
    Map<Long, String> nomes(Collection<Long> ids);

    boolean existe(long id);
}
