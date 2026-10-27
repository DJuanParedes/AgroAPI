# Trazabilidad real

El proyecto comienza el 27 de octubre de 2026 y se desarrolla con asistencia de IA.
No se altera la fecha de los commits ni se presenta este historial como trabajo de semanas anteriores.
La rúbrica exige historial previo distribuido en la unidad: ese criterio no puede acreditarse retroactivamente.

Cada funcionalidad tiene un issue, rama y pull request hacia develop. Las fusiones usan un commit de merge.
Los mensajes siguen feat/fix/refactor/test/docs, en español y en presente.

| Rama | Contenido | Verificación |
|---|---|---|
| feature/seguridad-jwt | BCrypt, JWT, filtro, privilegios, 401/403 | compilación y pruebas HTTP |
| feature/crud-labores | lotes, CRUD, RN-01..04, semillas e idempotencia | pruebas de integración |
| feature/pruebas-contrato | suite, colección API y documentación | test y bootJar |

Al integrar el producto se abre un PR de develop a main y se etiqueta el commit comprobado.
La solicitud SC se conserva en una rama separada cuando el docente comunica la letra.

## Límites de la entrega

La defensa técnica es individual. El estudiante debe recorrer el código y explicar las decisiones.
No se garantiza una calificación por la sola existencia de archivos o commits.
