/**
 * Módulo de acompanhamento do crescimento das crianças (medidas, curvas da OMS e fotos).
 *
 * <p>É autocontido para poder virar um produto separado: não importa nada de outros pacotes do casa-api, guarda os
 * dados no schema {@code crescimento} do Postgres e as fotos atrás de {@link com.kowalski.casaapi.crescimento.foto.FotoStorage}. A única ligação
 * com o resto do sistema são as interfaces de {@link com.kowalski.casaapi.crescimento.identidade}, implementadas fora daqui (no casa, pelo login
 * por pessoa). O teste {@code ArquiteturaCrescimentoTest} garante essa separação.
 */
package com.kowalski.casaapi.crescimento;
