package com.kowalski.casaapi.crescimento.identidade;

/**
 * Quem está usando o módulo. Implementado fora do módulo: no casa, pelo login por pessoa; num produto separado,
 * pelo login próprio.
 */
public interface UsuarioAtual {

    /** Id do usuário logado. Lança exceção (401/403) se não houver login. */
    long id();
}
