(ns back-end.servidor-http.servidor
  (:require
    [io.pedestal.http.route :as route]
    [io.pedestal.http :as http]
    [io.pedestal.test :as test])
  (:import (java.util UUID)))

;Essa função representará o primeiro endpoint na aplicação.
(defn funcao-hello
  "Essa função será executada e retornará o body com a mensagem 'Hello World'."
  [request]
  {:status 200 ;Esse será o status de retorno.
   :body   (str "Hello World, " (get-in request [:query-params :name] " Retorno padrão caso nenhum parâmetro 'name' seja enviado!")) ;Obtendo o parâmetro "name" que será enviado via request parameters pela URL.
   }) ;Esse será o retorno do end-point.

;Esse mapa representará um banco de dados em memória.
;{id {tarefa_id tarefa_nome tarefa_status}
;{id {tarefa_id tarefa_nome tarefa_status}}
(def store (atom {})) ;O "store" é um "atom", ou seja, um símbolo que pode ser alterado. Ele não possui a característica de imutabilidade, assim, podemos alterar o estado desse símbolo. Ele será o "banco de dados" em memória da aplicação.

;;Endpoint de criar tarefa
(defn cria-item-de-tarefa-no-mapa ;Essa função criará um item de tarefa com os dados que foram fornecidos na requisição.
  "Essa função criará um item de tarefa, que será retornado. Esse item poderá ser inserido no símbolo 'store' que representa o banco de dados."
  [nome status tarefa]
  {:nome nome :status status :tarefa tarefa}) ;Esse item de tarefa será o retorno dessa função.

(defn cria-tarefa
  "Essa função será chamada quando o endpoint de '/tarefa' for chamado."
  [request]
   (let [uuid (UUID/randomUUID)
         nome (get-in request [:query-params :nome])
         status (get-in request [:query-params :status])
         tarefa (get-in request [:query-params :tarefa])]
     (swap! store assoc uuid (cria-item-de-tarefa-no-mapa nome status tarefa)) ;O "swap" possibilita a alteração de valor em um símbolo, que deve ser um "atom". Assim, por padrão, estamos "quebrando" a imutabilidade do Clojure nesse símbolo. Isso é necessário para simularmos um banco de dados.
     {:status 201 :body {:mensagem "A tarefa foi registrada com sucesso!"
                         :tarefa tarefa}})
   )

;O endpoint abaixo será responsável por listar as tarefas que foram criadas.
(defn lista-tarefas
  "Esse endpoint listará todas as tarefas que foram cadastradas."
  [request]
  {:status 200 :body @store})

(def routes (route/expand-routes #{["/hello-world" :get funcao-hello :route-name :hello-world]
                                   ["/tarefa" :post cria-tarefa :route-name :criar-tarefa]
                                   ["/tarefa" :get lista-tarefas :route-name :lista-tarefas]}))

(def service-map
  {::http/routes routes
   ::http/port   9999    ;Essa será a porta padrão da aplicação.
   ::http/type   :jetty  ;Esse será o servidor utilizado.
   ::http/join?  false}) ;Em ambiente de dev, essa configuração é importante para o Clojure não travar a thread ao iniciar o processo, assim, podemos continuar testando a aplicação. Em ambiente de produção, não precisamos dessa configuração, pois se a thread morrer, o serviço deverá morrer também.


(http/start (http/create-server service-map))

;(def server (atom nil))
;(test/response-for (::http/service-fn @server) :get "/hello-world")

(println "O servidor HTTP foi inicializado!")
