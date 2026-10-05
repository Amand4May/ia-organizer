# Financeiro v2 — validação da cópia para teste

Data: 19/09/2026. Base do GitHub: `fea0aa38ed1808a939b5cb0ae130fe11d9f84435` (`Amand4May/ia-organizer`, `master`). O fetch confirmou a mesma revisão. Nenhum push, commit remoto, publicação ou alteração em Firebase/Groq foi realizado.

A evolução desta versão ficou no Financeiro, seus recursos visuais, cálculos, testes e documentação. Início, Agenda, login, perfil, navegação compartilhada e configuração Firebase mantêm os mesmos arquivos da cópia anterior. A verificação de escopo está em `validation-results/finance-v2-scope.json`.

## Resultados

| Verificação | Resultado e alcance |
|---|---|
| Domínio e persistência Java | **891 verificações aprovadas** nas classes usadas pelo app, incluindo os cenários da versão anterior. |
| `assembleLocalDebug` | APK compilado com Gradle 9.5.0, AGP 9.3.0, JDK 25.0.2 e SDK 37. |
| `assembleConnectedDebug` | APK compilado com Firebase e permissão de rede. Compilar não comprova autenticação ou gravação remota. |
| `testLocalDebugUnitTest` | **13 testes JUnit/Robolectric aprovados**, sem falhas ou testes ignorados. O teste de regressão inclui as 891 verificações acima; não são 891 testes de interface. |
| Interface | Cinco testes de comportamento com Views Android e renderização nativa do Robolectric, API 35, 393 × 852 dp. Capturas em `preview/`. |
| Recursos e sintaxe | 46 XMLs/referências locais verificados; compilação Android resolveu os tipos e recursos. |
| Isolamento local | Manifesto final sem INTERNET e sem FirebaseInitProvider, pacote `com.example.organizadoria.local`. |
| Variante conectada | Pacote original, INTERNET e FirebaseInitProvider presentes. |
| API CDI pública | Endpoint SGS 12 consultado; resposta e data registradas em `validation-results/bcb-cdi-observation.json`. JSON e cálculos verificados. |
| Cliente CDI no aparelho | **Pendente**. Sondas Java pela rede deste ambiente tiveram timeout; a consulta pública por outro cliente respondeu. Não equivale a testar a chamada dentro do APK em um telefone. |
| Firebase/Groq reais | **Não executados** contra sua conta. Nenhum dado financeiro real foi usado. |
| Aparelho/emulador, API mínima e API 37 | **Pendente**. As capturas são do Robolectric, não de um emulador Android completo. |
| Distribuição comercial | Ainda depende da aceitação no aparelho, integração real, regras do Firestore e configuração de release/assinatura do projeto. |

O build e os resultados JUnit acompanham o projeto em `validation-results/`. As imagens usam dados sintéticos, inclusive a cotação injetada no teste visual de CDI.

Para resolver as ferramentas neste ambiente, uma cópia de validação usou o endereço alternativo oficial do Maven Central (`repo1.maven.org`) e o JDK já instalado em lugar do plugin de download automático. As versões e configurações correspondentes do projeto entregue foram preservadas. Esses ajustes não fazem parte do app.

## O que os testes exercitam

- Salário de R$3.000, contas de R$800 e plano de aporte de R$500: disponível R$1.700.
- Aporte parcial de R$350: R$150 pendentes, sem virar despesa nem duplicar deduções.
- Despesa prevista de R$45 liquidada por R$40: diferença liberada.
- Salário previsto/realizado/parcial, recorrências, datas 29–31, fechamento, transporte e destinação da sobra.
- Metas, reservas, transferências, resgates, estornos, correções, idempotência e persistência transacional.
- CDI diário convertido em taxa anual; percentual contratado aplicado diariamente, não como simples multiplicação da taxa anual.
- Projeção de carteira com taxas diferentes por aplicação e aporte mensal na aplicação principal.
- Cálculo inverso de aporte; taxa zero/negativa; prazos e respostas da API inválidos; data futura e cotação antiga.
- Interpretação de CDI, saldo total versus aporte, conta nova, confirmação, rollback e preservação dos dados.
- Páginas financeiras, cadastro preservado na rotação, simulações sem gravação, despesa confirmada e aporte parcial pelo formulário.
- JSON sintético da IA e ligação Agenda/despesa na lógica compartilhada, sem chamada ao Groq.

Referências calculadas: R$8.000 + R$500/mês a 8% a.a. resultam em R$48.226,95 em cinco anos. Com R$700/mês, R$62.815,88. Para R$100 mil no mesmo prazo e taxa, partindo de R$8.000, o aporte estimado é R$1.209,76/mês. São estimativas brutas e não retornos garantidos.

## Repetir no Android Studio

Na raiz do projeto, no Terminal do Android Studio para Windows:

```powershell
.\gradlew.bat :app:assembleLocalDebug :app:assembleConnectedDebug :app:testLocalDebugUnitTest
```

Com emulador/celular aberto e a variante local:

```powershell
.\gradlew.bat :app:connectedLocalDebugAndroidTest
```

Os quatro testes de instrumentação em `androidTestLocal` continuam disponíveis para verificar isolamento, navegação, aporte, recriação e Agenda/comando no Android completo. Eles não foram executados nesta entrega.

## Aceitação no seu aparelho

1. Abra Início e Agenda e confira o fluxo habitual. Entre no Financeiro pela navegação.
2. Use os números do exemplo de R$3.000 / R$800 / R$500; confira o disponível de R$1.700. Não confirme novamente um salário já incluído no saldo inicial.
3. Planeje R$45, confirme R$40; registre R$350 de um aporte previsto de R$500. Confira a diferença e o patrimônio.
4. Crie uma meta, reserve valores existentes, feche um ciclo e escolha o destino da sobra. Reabra o app e confira persistência.
5. Em Investimentos, altere prazo, aporte simulado e taxa. Compare cenários e consulte a tabela mensal; confirme que o saldo não mudou.
6. Em `connectedDebug`, entre com uma conta de teste; confira leitura/gravação em `users/{uid}/financeiro_v1`, acesso apenas ao próprio usuário e tratamento de falta de conexão.
7. Confira CDI real, data exibida, atualização e falha de rede. A API não exige chave. O modo local usa taxa personalizada.
8. Configure sua chave Groq em `local.properties`, experimente as frases e confira a prévia antes de confirmar. Teste cancelamento, rotação, teclado e fonte ampliada.

Esta entrega é uma candidata para teste. Build e testes automatizados não garantem funcionamento integral de serviços externos nem substituem a homologação comercial.
