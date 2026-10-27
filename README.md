# AgroAPI

Servicio de AgroSierra para el Examen Parcial II, Unidad 2.

Desarrollo iniciado el 27 de octubre de 2026. El historial conserva las fechas reales.
Framework: Spring Boot con Kotlin. Aplicación compañera: AgroCampo.

## Ejecutar

Requisitos: JDK 21. En Windows, configure JAVA_HOME y ejecute un comando:

```powershell
.\gradlew.bat bootRun
```

Alternativa con Docker: `docker compose up --build`.
Servicio: `http://localhost:8080/api/v1`. El emulador Android accede a `http://10.0.2.2:8080/api/v1/`.
La base H2 relacional persiste en `data/`; no es un servicio simulado. La semilla se carga solo si no hay lotes.
Para producción configure DB_URL, DB_USER, DB_PASSWORD y JWT_SECRET (mínimo 32 bytes).
Sin JWT_SECRET se genera una clave criptográfica local persistente en data/jwt.key, excluida de Git.

## Usuarios semilla

Contraseña de demostración: `Agro2026*` (almacenada como hash BCrypt).

| Usuario | Rol | Privilegios |
|---|---|---|
| tecnico1 | TECNICO | LOTE_LEER, LABOR_LEER, LABOR_CREAR, LABOR_EDITAR (propias y dentro de 24 h) |
| jefe1 | JEFE_CAMPO | lectura, creación, edición de todas, eliminación y CAMPANIA_CERRAR |
| admin | ADMINISTRADOR | lectura y USUARIO_GESTIONAR |

LABOR_EDITAR_TODAS es una capacidad adicional asignada al jefe en la base: permite implementar RN-02 sin comparar nombres de rol.
El privilegio USUARIO_GESTIONAR queda asignado; el contrato del examen no incluye endpoints de administración de usuarios.
Semillas: 8 lotes en Mantaro, Sagrado y Colca; lotes 7 y 8 cerrados; 20 labores; lote 1 con cosecha y lote 2 sin cosecha.

## Contrato REST

Todas las rutas excepto login requieren `Authorization: Bearer <token>`.

| Verbo | Ruta bajo /api/v1 | Resultado |
|---|---|---|
| POST | /auth/login | 200 con token, usuario, nombre, rol y privilegios; 401 inválido |
| GET | /usuarios/me | 200 con usuario actual |
| GET | /lotes?pagina=0&tamanio=20&valle=Mantaro | listado paginado; filtro opcional campaniaCerrada=true/false |
| GET | /lotes/{id} | detalle o 404 |
| GET | /lotes/{id}/labores?pagina=0&tamanio=20 | listado por fecha descendente |
| POST | /labores | 201 con ID definitivo y Location |
| PUT | /labores/{id} | 200 con labor corregida |
| DELETE | /labores/{id} | 204 sin cuerpo |
| POST | /lotes/{id}/cerrar-campania | 200 con lote actualizado o 409 |

Login: `{"usuario":"tecnico1","contrasena":"Agro2026*"}`.
Labor: `{"loteId":2,"tipo":"RIEGO","fecha":"2026-10-27","cantidad":2.0,"observacion":"Riego de prueba"}`.
Se admiten RIEGO, FUMIGACION y COSECHA. El responsable se toma de la identidad autenticada.

Paginación: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`.
Errores: `timestamp`, `status`, `error`, `message`, `path`, `validationErrors`.
Se distinguen 400, 401, 403, 404, 409 y 500. Una eliminación 204 nunca devuelve JSON.

## Arquitectura y seguridad

`config/`: SecurityConfig, JwtAuthenticationFilter, JwtService, ManejadorDeErrores y semillas.
`seguridad/`: Usuario, Rol, Privilegio, UsuarioRepository y AuthController.
`dominio/`: Lote, Labor, TipoLabor. `repositorio/`: SQL parametrizado con JdbcTemplate.
`servicio/`: RN-01 a RN-04 y transacciones. `web/` y `web/dto/`: transporte HTTP.

Los privilegios se cargan de rol_privilegio y se verifican con hasAuthority; el servicio también aplica propiedad y plazo de edición.
Los JWT se verifican con firma, emisor y expiración de una hora. No hay renovación.
Los bloqueos de lote coordinan las escrituras con el cierre de campaña.
POST admite Idempotency-Key: si una respuesta se pierde, un reintento devuelve la misma labor.

## Verificación y entrega

```powershell
.\gradlew.bat test bootJar
git log --graph --oneline --all
git log --date=short --pretty="%h %ad %s"
```

Pruebas: autenticación, firma/expiración, privilegios, paginación, RN-01..04 y CRUD persistido.
Colección exportable: `docs/AgroAPI.postman_collection.json`.
Consulte `docs/TRAZABILIDAD.md` para ramas y limitaciones reales del historial.
