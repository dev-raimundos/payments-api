# payments-api

API REST para recebimento e gerenciamento de pagamentos de débitos de pessoas físicas e jurídicas.

## Tecnologias

- Java 17
- Spring Boot 3.5.12
- Spring Data JPA
- H2 Database (em memória)
- Lombok
- Springdoc OpenAPI (Swagger UI)

## Como executar

### Pré-requisitos

- Java 17+
- Maven 3.8+

### Rodando a aplicação

```bash
git clone https://github.com/dev-raimundos/payments-api.git
cd payments-api
./mvnw spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`.

### Documentação interativa

Acesse o Swagger UI para explorar e testar todos os endpoints:

```
http://localhost:8080/swagger-ui.html
```

### Console do banco de dados H2

```
http://localhost:8080/h2-console
```

| Campo     | Valor                    |
|-----------|--------------------------|
| JDBC URL  | `jdbc:h2:mem:paymentsdb` |
| User Name | `sa`                     |
| Password  | *(deixe em branco)*      |

---

## Endpoints

### Criar pagamento
`POST /payments`

```json
{
  "debtCode": 1001,
  "cpfCnpj": "123.456.789-00",
  "paymentMethod": "pix",
  "amount": 150.00
}
```

Métodos de pagamento aceitos: `boleto`, `pix`, `cartao_credito`, `cartao_debito`.

> O campo `cardNumber` é obrigatório apenas para `cartao_credito` e `cartao_debito`.

---

### Atualizar status do pagamento
`PATCH /payments/{id}/status`

```json
{
  "status": "PROCESSADO_SUCESSO"
}
```

Regras de transição de status:

| Status atual               | Pode ir para                                      |
|----------------------------|---------------------------------------------------|
| `PENDENTE_PROCESSAMENTO`   | `PROCESSADO_SUCESSO` ou `PROCESSADO_FALHA`        |
| `PROCESSADO_FALHA`         | `PENDENTE_PROCESSAMENTO`                          |
| `PROCESSADO_SUCESSO`       | *(nenhuma alteração permitida)*                   |
| `INATIVO`                  | *(nenhuma alteração permitida)*                   |

---

### Listar pagamentos
`GET /payments`

Suporta filtros opcionais via query params:

| Parâmetro  | Exemplo                          |
|------------|----------------------------------|
| `debtCode` | `/payments?debtCode=1001`        |
| `cpfCnpj`  | `/payments?cpfCnpj=123.456.789-00` |
| `status`   | `/payments?status=PENDENTE_PROCESSAMENTO` |

---

### Inativar pagamento (exclusão lógica)
`DELETE /payments/{id}`

Remove logicamente o pagamento, alterando seu status para `INATIVO`.

> Apenas pagamentos com status `PENDENTE_PROCESSAMENTO` podem ser inativados.

---

## Estrutura do projeto

```
src/main/java/com/fadesp/paymentsapi
├── controller      # Endpoints REST
├── service         # Regras de negócio
├── repository      # Acesso ao banco de dados
├── model           # Entidade JPA
├── dto             # Objetos de entrada e saída
├── enums           # PaymentMethod e PaymentStatus
└── exception       # Exceptions customizadas e handler global
```

---

## Autor

Raimundos Marques da Silva Neto
[github.com/dev-raimundos](https://github.com/dev-raimundos)