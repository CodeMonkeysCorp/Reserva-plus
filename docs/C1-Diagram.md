@startuml C1_ReservaPlus
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

title C1 - System Context - Reserva+

Person(user, "Usuário", "Morador/associado que realiza reservas")
Person(admin, "Administrador", "Gerencia espaços e regras do sistema")

System(reserva, "Reserva+", "Sistema web de gestão e agendamento de quadras e quiosques")

Rel(user, reserva, "Realiza reservas, cancela e consulta")
Rel(admin, reserva, "Gerencia espaços e horários")

@enduml