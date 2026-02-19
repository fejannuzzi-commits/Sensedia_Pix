Este repositório contém:
- **backend/**: Spring Boot 3 (Java 17)
- **mock-fraud/**: Mock da API de Fraude (porta 9090)
- **mock-bacen/**: Simulador BACEN (consome pix.send.queue e publica pix.response)
- **frontend/**: React + Vite

# PIX Platform


## BACEN mock - tunáveis (docker-compose env)
No serviço `bacen-mock` você pode ajustar:

- `PIX_MIN_DELAY_MS` / `PIX_MAX_DELAY_MS` (latência variável)
- `PIX_TIMEOUT_RATE` e `PIX_TIMEOUT_DELAY_MS` (respostas muito atrasadas)
- `PIX_ERROR_RATE` (envia `reasonCode=SIMULATE_ERROR` para acionar retry/DLQ no backend)
- `PIX_MALFORMED_RATE` (publica JSON inválido para testar parse error + retry/DLQ)
- `PIX_DROP_RATE` (não responde)
- `PIX_DUPLICATE_RATE` (resposta duplicada)
