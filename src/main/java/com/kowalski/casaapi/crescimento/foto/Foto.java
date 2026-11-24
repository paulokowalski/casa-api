package com.kowalski.casaapi.crescimento.foto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "foto")
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID familiaId;

    @Column(nullable = false)
    private UUID criancaId;

    /** Prefixo dos arquivos no armazenamento; o arquivo de cada variante é chave + {@link Variante#sufixo()}. */
    @Column(nullable = false, length = 200)
    private String chave;

    @Column(nullable = false)
    private Integer largura;

    @Column(nullable = false)
    private Integer altura;

    /** Soma do tamanho das variantes gravadas. */
    @Column(nullable = false)
    private Long bytes;

    /** Do arquivo enviado: evita guardar a mesma foto duas vezes. */
    @Column(nullable = false, length = 64)
    private String sha256;

    /** Data em que a foto foi tirada (EXIF), quando o arquivo traz. */
    private LocalDateTime tiradaEm;

    private Long enviadaPor;

    /** Registro do diário/álbum em que a foto está; null = foto solta (ex.: só de perfil). */
    private UUID registroId;

    /** Posição dentro do registro. */
    private Short ordem;

    @Column(name = "criado_em", nullable = false)
    private Instant criadaEm = Instant.now();
}
