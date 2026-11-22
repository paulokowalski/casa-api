package com.kowalski.casaapi.crescimento.identidade;

import java.util.Collection;
import java.util.Map;

/** Consulta de usuários para mostrar nomes (membros da família, quem registrou). Implementado fora do módulo. */
public interface DiretorioUsuarios {

    /** Nome de cada id conhecido; ids inexistentes ficam de fora do mapa. */
    Map<Long, String> nomes(Collection<Long> ids);

    /** Se existe um usuário ativo com esse id. */
    boolean existe(long id);
}
