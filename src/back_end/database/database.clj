(ns back-end.database.database)

;Esse mapa representará um banco de dados em memória.
;{id {tarefa_id tarefa_nome tarefa_status}
;{id {tarefa_id tarefa_nome tarefa_status}}

(def store (atom {})) ;O "store" é um "atom", ou seja, um símbolo que pode ser alterado. Ele não possui a característica de imutabilidade, assim, podemos alterar o estado desse símbolo. Ele será o "banco de dados" em memória da aplicação.