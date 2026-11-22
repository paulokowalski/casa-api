package com.kowalski.casaapi.crescimento.ia;

import java.util.List;

/**
 * A IA do módulo. Faz só duas tarefas: o resumo do mês e responder perguntas dos pais sobre a criança naquele mês
 * (recusando outros assuntos). As instruções e o formato da resposta ficam do lado de quem gera (no casa, o container
 * casa-ia, com a assinatura pessoal do Claude); daqui vão apenas dados em texto e a pergunta.
 * Num produto separado, troca-se por uma implementação com chave de API, sem mexer no resto do módulo.
 */
public interface GeradorIa {

    record Resultado(String resumo, List<String> perguntas, String modelo) {
    }

    record Resposta(boolean foraDoAssunto, String resposta, String modelo) {
    }

    boolean disponivel();

    /**
     * Resumo do mês e perguntas para o pediatra a partir dos dados (sem fotos, sem nomes, sem links).
     *
     * @throws com.kowalski.casaapi.crescimento.comum.IaIndisponivelException se não der
     */
    Resultado resumirMes(String dados);

    /**
     * Responde uma pergunta dos pais com base nos dados do mês. {@code foraDoAssunto} quando a pergunta não é sobre a
     * criança e seus cuidados.
     *
     * @throws com.kowalski.casaapi.crescimento.comum.IaIndisponivelException se não der
     */
    Resposta responder(String dados, String pergunta);
}
