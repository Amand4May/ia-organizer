# Cogni — Financeiro v2 para teste

Esta cópia contém a implementação da área financeira independente, com HUD, ciclo entre salários, investimentos, metas, extrato e planejamento. O fluxo de entrada existente foi mantido: Login → Início, com Agenda e interação por comandos. O Financeiro é acessado pela navegação já existente.

Esta é a versão 2 da cópia para teste. A base do GitHub foi consultada novamente; nenhum commit ou push foi enviado. A experiência principal e a Agenda da cópia anterior foram preservadas. Os resultados de validação e os limites da entrega estão em [docs/FINANCEIRO_VALIDACAO.md](docs/FINANCEIRO_VALIDACAO.md).

## Novidades

- HUD com disponível em destaque, distribuição do dinheiro e atalhos para as áreas internas.
- Cadastro em três etapas curtas. Salário, aporte e contas têm ajustes separados.
- **Conte ao Cogni:** frases com prévia e confirmação antes de salvar; nada é registrado automaticamente pela IA.
- Curvas interativas de investimentos; detalhes mensais; 1, 5, 10 anos ou prazo personalizado; comparação de cenários; cálculo do aporte para atingir uma meta.
- Taxa por aplicação: percentual do CDI ou taxa anual personalizada. A simulação da carteira considera cada taxa e o aporte mensal na aplicação principal.
- CDI do Banco Central, com data da observação, cache e aviso quando a taxa está antiga. Não há taxa inventada se a consulta falhar.
- Planejamento visual de caixa por 90 dias, filtros rápidos no extrato e calendário nos formulários.

## Abrir no Android Studio

1. Extraia o ZIP em uma pasta nova. Abra a pasta **ia-organizer**, que contém `settings.gradle.kts` e `gradlew.bat`.
2. Aguarde a sincronização. As versões originais de Gradle, AGP, JDK e SDK foram mantidas.
3. Em **Build Variants**, escolha **localDebug** para dados separados e sem rede, ou **connectedDebug** para Firebase e CDI reais.
4. Selecione o emulador/celular no topo e clique em ▶ na configuração **app**.
5. Entre no Financeiro. Uma instalação vazia oferece o cadastro inicial; os dados financeiros existentes da mesma variante são preservados.

A versão conectada mantém o `app/google-services.json` original. Seus registros confirmados nela usam o Firebase da sua conta; executar o app não publica código no GitHub. Login, permissões do Firestore e uma chamada real à IA precisam ser conferidos no seu ambiente.

Para a IA remota, mantenha sua chave no arquivo **local.properties** da raiz, junto da linha `sdk.dir` criada pelo Android Studio:

```properties
GROQ_API_KEY=SUA_CHAVE_LOCAL
```

Não use aspas e não envie esse arquivo ao GitHub. A chave não está incluída no ZIP. Sem ela, o Financeiro usa o interpretador local dos exemplos e mostra essa condição na tela. O CDI público não precisa de chave.

O APK local fornecido junto do projeto permite testar sem login remoto. Se você já instalou uma compilação local feita no seu Android Studio e houver conflito de assinatura, compile este projeto no mesmo Android Studio. Não desinstale a versão anterior apenas para contornar a assinatura, pois isso apaga os dados locais dela.

## Investimento por frase

No Financeiro, toque em **Conte ao Cogni**:

```text
Tenho R$8.000 a 110% do CDI e aporto R$500 por mês por 5 anos
Investi R$350 a 100% do CDI
Simule R$8.000 a 100% do CDI por 5 anos
Gastei R$40 no corte
Quanto terei em 5 anos?
Quanto preciso investir para chegar a R$100 mil em 5 anos?
```

Ao registrar CDI, escolha entre **saldo total que já possuo** e **novo aporte**, além da aplicação de destino. Essa distinção evita duplicar patrimônio. O plano mensal só muda se você marcar essa opção. Uma frase sem percentual usa 100% do CDI como hipótese indicada para revisão. No modo local, teste os gráficos com taxa personalizada; a consulta automática exige `connectedDebug`.

Fonte pública: [Banco Central, série SGS 12](https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados/ultimos/10?formato=json). A unidade recebida é percentual diário por dia útil. O percentual contratado é aplicado à taxa diária antes da capitalização. A projeção mantém essa observação constante, com 252 dias úteis/ano e 21/mês; não prevê alterações futuras de juros nem usa calendário de feriados.

As estimativas são **brutas**, sem impostos, custos ou inflação, com aportes ao fim do mês. Não são retornos garantidos e não atualizam o saldo real. A data da observação é mostrada e valores com mais de sete dias são destacados.

## O que foi implementado

| Área | Funcionalidades |
|---|---|
| Visão geral / HUD | Disponível em destaque; saldo, comprometido, investido e patrimônio; aportes pendentes; reservas; próximos eventos financeiros e acessos às demais páginas. |
| Ciclo | Salário líquido e dia de recebimento; confirmação do recebimento; recebimentos parciais; distribuição da renda; receitas e despesas realizadas; histórico de fechamentos e destino da sobra. |
| Investimentos | Posição atual por conta; aporte fixo ou percentual do salário; planejado, parcial e realizado; resgates; atualização da posição; projeções para 1, 5, 10 anos e prazo personalizado; comparação de aportes e taxas; cálculo inverso para atingir uma meta. |
| Metas | Criação, edição, prazo, objetivo, progresso, destinação de valores já existentes, liberação de reservas, exclusão e simulação do aporte necessário. |
| Extrato | Receitas, despesas, aportes, transferências e atualizações de saldo; planejados e realizados; busca por descrição, período e tipo; estorno e correção com histórico; previsões canceladas. |
| Planejamento | Despesas futuras, previsões de receita e aporte, contas recorrentes, alterações de recorrência e projeção de caixa para 90 dias. |
| Contas | Contas de movimentação e aplicações, saldos iniciais, transferências internas e reconciliação de saldos. |
| Agenda + comandos | Um compromisso com custo gera Agenda + previsão financeira na mesma transação. Um gasto posterior concilia a previsão; havendo mais de uma candidata, o usuário escolhe. |
| Histórico anterior | Registros antigos podem ser revisados sem apagar os documentos de origem. Valores anteriores do perfil ficam disponíveis para conferência. Nada é somado automaticamente ao novo saldo. |

Os layouts de Início, Agenda, Perfil, login conectado e a navegação inferior foram reaproveitados. A área Financeiro usa Fragment, Material Cards, botões, cores e fundo escuro. As funcionalidades detalhadas ficam nas páginas internas e em formulários, sem concentrar gráficos no HUD.

## Como experimentar sem tocar no aplicativo original

Abra este projeto no Android Studio e selecione **`localDebug`** em Build Variants. Essa variante tem:

- Nome **Cogni Local** e pacote `com.example.organizadoria.local`.
- Entrada local sem conta Firebase.
- Armazenamento privado separado e persistente entre aberturas.
- Permissão INTERNET removida e inicialização automática do Firebase removida do manifesto local.
- Configuração Firebase fictícia, sem credenciais de produção.
- Interpretador de comandos local para os exemplos financeiros, sem chamada a uma IA externa.

Depois de entrar, abra Financeiro → Configurar meu financeiro. Informe saldos existentes e o salário previsto. Registrar salário recebido acrescenta dinheiro ao saldo: não registre novamente valores já incluídos no saldo inicial. Para um aporte sobre dinheiro que já estava na conta, crie uma previsão avulsa em Planejamento.

Exemplos aceitos pelo interpretador local:

```text
Todo dia 5 recebo R$3.000 e quero investir R$500
Recebi R$3.000 de salário
Amanhã vou cortar o cabelo às 9h por R$45
Gastei R$40 no corte
Investi R$350
Todo mês quero investir 10% do salário
Tenho R$8.000 investidos, aporto R$500/mês e espero retorno médio de 8% a.a.
E se eu investir R$700 por mês?
Quanto terei em 5 anos?
Quanto preciso investir mensalmente para chegar a R$100 mil em 5 anos?
```

O interpretador local reconhece padrões; não oferece toda a compreensão de linguagem do Groq. Comandos não reconhecidos mostram uma mensagem e preservam o texto. Os formulários permitem usar as funcionalidades financeiras independentemente dos comandos.

A variante `connected` mantém Firebase/Groq e o CDI público. Valide primeiro usando uma conta de teste.

## Regras de cálculo

Valores monetários são armazenados em centavos (`long`). Os cálculos de movimentações não usam `double`.

```text
Saldo       = dinheiro nas contas de movimentação
Comprometido = despesas ainda pendentes antes do próximo salário, incluindo atrasadas
Disponível  = saldo − comprometido − aportes pendentes − reservas em contas
Investido   = posições existentes nas contas de investimento
Patrimônio  = saldo + investido
```

Uma reserva para meta aponta para dinheiro que já existe. Não cria outro ativo. Um aporte transfere de uma conta para um investimento: reduz o saldo, aumenta o investido e preserva o patrimônio. Resgates fazem o movimento inverso. Atualizações da posição investida ficam separadas dos aportes e das despesas.

O salário cadastrado é uma previsão. Somente a confirmação de recebimento cria receita realizada e reserva o aporte do ciclo. O percentual é calculado sobre o salário efetivamente confirmado. Receitas extras não abrem outro ciclo. Ao confirmar um novo salário principal, um ciclo anterior ainda aberto é fechado com a sobra carregada; o fechamento manual oferece os demais destinos.

Contas previstas no próprio dia do próximo recebimento pertencem ao próximo período. Contas atrasadas permanecem comprometidas até pagamento ou cancelamento. Dias 29–31 inexistentes usam o último dia do mês; não há ajuste automático para feriados ou dias úteis. As recorrências mantêm um horizonte de 12 meses, renovado ao usar o Financeiro.

Uma despesa de R$45 liquidada por R$40 encerra a previsão e libera R$5. A opção de pagamento parcial preserva o restante. Aportes são parciais por padrão: R$350 realizados sobre R$500 planejados deixam R$150 pendentes. Estorno e substituição acontecem atomicamente; um ciclo fechado não é reescrito.

Fechar um ciclo não cria receita. Saldo, contas pendentes e reservas continuam existindo. O retrato do fechamento guarda receitas, despesas, aportes, resgates, ajustes, compromissos, reservas, sobra antes da destinação e valor destinado. É possível iniciar outro ciclo no mesmo dia sem reabrir o histórico fechado.

As projeções usam taxa anual efetiva convertida para mensal e aportes ao fim do mês. São estimativas nominais sem impostos, taxas ou inflação, com aviso de retorno não garantido. Não atualizam automaticamente o saldo real.

## Organização do código

| Local | Responsabilidade |
|---|---|
| `financeiro/domain/FinanceState.java` | Contas, previsões, lançamentos, ciclos, recorrências, metas, reservas e vínculo de Agenda. |
| `financeiro/domain/FinanceEngine.java` | Regras financeiras, validações e fechamento. |
| `financeiro/domain/Projection.java` | Juros compostos e cálculo inverso. |
| `financeiro/domain/CommandRouter.java` | Aplicação de comandos e conciliação entre Agenda e Financeiro. |
| `financeiro/domain/LocalCommandParser.java` | Interpretação local dos exemplos, sem rede. |
| `RemoteCommandCodec.java` | Contrato JSON da IA conectada; validação antes de qualquer gravação. |
| `financeiro/data/FileFinanceRepository.java` | Cópia local: gravação atômica, rollback, isolamento de usuário e persistência. |
| `financeiro/data/FirestoreFinanceRepository.java` | Adaptador conectado: documentos por entidade, revisão e transação com detecção de conflito. |
| `financeiro/ui/` | HUD, páginas, formulários e componentes visuais. |
| `AgendaFeed.java` | Compromissos novos e antigos, sem exibir lançamentos financeiros na Agenda. |
| `AppSession.java`, `AppServices.java` | Sessão, escolha do armazenamento, execução fora da thread principal e atualização das recorrências. |

A Agenda funciona sem configurar saldos. O registro compartilhado torna atômico um comando como “cabelo às 9h por R$45”: ou compromisso e previsão são salvos juntos, ou nenhum deles é salvo. A interface e a navegação das duas áreas permanecem separadas.

No adaptador conectado, as novas entidades ficam em `users/{uid}/financeiro_v1`; a coleção antiga `tarefas` continua sendo lida para os compromissos anteriores. A revisão financeira do legado não remove os documentos antigos. Cancelar explicitamente um compromisso legado na variante conectada mantém a operação de exclusão que já existia no aplicativo. Isso não foi executado nesta cópia.

## Testes

Verificações executáveis sem Android SDK, a partir da raiz do projeto, com JDK 17+ e Python 3:

```bash
bash tools/run_finance_tests.sh
java tools/CheckJavaSyntax.java
python3 tools/check_android_resources.py
git diff --check
```

Com o SDK e as dependências Android disponíveis:

```bash
bash gradlew :app:assembleLocalDebug :app:assembleConnectedDebug :app:testLocalDebugUnitTest
bash gradlew :app:connectedLocalDebugAndroidTest
```

O segundo comando usa aparelho/emulador. Os testes específicos desta entrega ficam em `androidTestLocal`, criam dados sintéticos e verificam a ausência de INTERNET. Não execute testes usando a variante `connected` contra serviços reais.

As versões originais de Gradle, AGP e SDK foram mantidas. Os dois APKs foram compilados e os testes JUnit/Robolectric passaram. O relatório distingue essas verificações dos testes em aparelho e serviços reais, que ainda precisam ser feitos.
