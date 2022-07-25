(ns back-end.servidor-http.servidor
  (:require
    [io.pedestal.http.route :as route]
    [io.pedestal.http :as http]
    [back-end.database.database :as database]
    [io.pedestal.interceptor :as i])
  (:import (java.util UUID)))

;Essas duas funções serão executadas antes das requisições e servirão para
;carregar o banco de dados diretamente na requisição que está sendo enviada.
(defn assoc-store
  "Estamos obtendo o banco de dados de um namespace externo e inserindo-o na chave ':store' da requisição, que está dentro do contexto recebido."
  [context]
  (update context :request assoc :store database/store))

(def db-interceptor
  "Essa função interceptará a função final, ela será como um 'filtro'. Ele retornará o nome da rota, que é 'db-interceptor' e executará a função 'assoc-store', que carregará o banco de dados, que está em memória, na
  requisição, que está dentro do contexto, através da chave ':store', assim, 'injetaremos' o banco de dados em memória
  diretamente na requisição que está sendo feita."
  {:nome :db-interceptor
   :enter assoc-store})

;Essa função representará o primeiro endpoint na aplicação.
(defn funcao-hello
  "Essa função será executada e retornará o body com a mensagem 'Hello World'."
  [request]
  {:status 200 ;Esse será o status de retorno.
   :body   (str "Hello World, " (get-in request [:query-params :name] " Retorno padrão caso nenhum parâmetro 'name' seja enviado!")) ;Obtendo o parâmetro "name" que será enviado via request parameters pela URL.
   }) ;Esse será o retorno do end-point.

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
         tarefa (get-in request [:query-params :tarefa])
         store (:store request)]
     (swap! store assoc uuid (cria-item-de-tarefa-no-mapa nome status tarefa)) ;O "swap" possibilita a alteração de valor em um símbolo, que deve ser um "atom". Assim, por padrão, estamos "quebrando" a imutabilidade do Clojure nesse símbolo. Isso é necessário para simularmos um banco de dados.
     {:status 201 :body {:mensagem "A tarefa foi registrada com sucesso!"
                         :tarefa tarefa}})
   )

;O endpoint abaixo será responsável por listar as tarefas que foram criadas.
(defn lista-tarefas
  "Esse endpoint listará todas as tarefas que foram cadastradas."
  [request]
  {:status 200 :body @(:store request)})

;O endpoint abaixo será responsável por deletar uma determinada tarefa.
(defn deleta-tarefa
  "Esse endpoint deletará uma determinada tarefa pelo ID."
  [request]
  (let [store (:store request) ;Estamos obtendo o banco de dados que está na memória e foi carregado pela função "assoc-store", chamada pelo "db-interceptor".
        tarefa-id (get-in request [:path-params :id])
        tarefa-id-convertida-para-uuid (UUID/fromString tarefa-id)] ;Como ao criar tarefas estamos armazenando um UUID, temos que converter o ID recebido pela string para um objeto que também está no formato UUID, caso contrário o Clojure não identificará que dois UUIDs são iguais por causa do tipo deles.
    (swap! store dissoc tarefa-id-convertida-para-uuid) ;Estamos utilizando a função "swap" para retirarmos a tarefa em que o ID foi passado.
    {:status 200 :body {:mensagem "Removido com sucesso!"}}))

;O endpoint abaixo será responsável por atualizar uma determinada tarefa.
(defn atualiza-tarefa
  "Esse endpoint atualizará uma determinada tarefa pelo ID."
  [request]
  (let [tarefa-id (get-in request [:path-params :id])
        tarefa-id-uuid (UUID/fromString tarefa-id)
        nome (get-in request [:query-params :nome])
        status (get-in request [:query-params :status])
        tarefa (get-in request [:query-params :tarefa])
        tarefa-criada (cria-item-de-tarefa-no-mapa nome status tarefa)
        store (:store request)]
    (swap! store assoc tarefa-id-uuid tarefa-criada)
    {:status 201 :body {:mensagem "A tarefa foi atualizada com sucesso!"
                        :tarefa tarefa-criada}}))

(def routes (route/expand-routes #{["/hello-world" :get funcao-hello :route-name :hello-world]
                                   ["/tarefa" :post [db-interceptor cria-tarefa] :route-name :criar-tarefa] ;O "db-interceptor" será chamado antes da requisição para criar as tarefas. Ele colocará o "store", que é o nosso banco de dados, dentro da request, bastando apenas inserirmos a nova tarefa nesse mapa que foi injetado dentro da request..
                                   ["/tarefa" :get [db-interceptor lista-tarefas] :route-name :lista-tarefas]
                                   ["/tarefa/:id" :delete [db-interceptor deleta-tarefa] :route-name :deleta-tarefa]
                                   ["/tarefa/:id" :patch [db-interceptor atualiza-tarefa] :route-name :atualiza-tarefa]}))

(def service-map
  {::http/routes routes
   ::http/port   9999    ;Essa será a porta padrão da aplicação.
   ::http/type   :jetty  ;Esse será o servidor utilizado.
   ::http/join?  false}) ;Em ambiente de dev, essa configuração é importante para o Clojure não travar a thread ao iniciar o processo, assim, podemos continuar testando a aplicação. Em ambiente de produção, não precisamos dessa configuração, pois se a thread morrer, o serviço deverá morrer também.


(def service-map-com-interceptor
  "Esse service-map já está com um interceptor inserido, assim, ele servirá para todas
  as rotas, e não será mais necessário a inserção de um interceptor, de forma manual, em
  cada rota da aplicação."
  (-> service-map
      (http/default-interceptors)
      (update ::http/interceptors conj (i/interceptor db-interceptor))))

(http/start (http/create-server service-map-com-interceptor))

;(def server (atom nil))
;(test/response-for (::http/service-fn @server) :get "/hello-world")

(println "O servidor HTTP foi inicializado!")
