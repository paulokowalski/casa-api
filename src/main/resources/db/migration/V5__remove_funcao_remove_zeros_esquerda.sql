-- A função era usada só nos filtros antigos de mês/ano (date_part + texto),
-- substituídos por intervalo de datas. Nenhuma consulta usa mais.
DROP FUNCTION IF EXISTS remove_zeros_esquerda(text);
