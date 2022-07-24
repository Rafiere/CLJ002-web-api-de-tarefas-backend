(ns back-end.core
  (:require
  [io.pedestal.http.route :as route]
  [io.pedestal.http :as http]))

(defn funcao-hello
  "Essa função será executada e retornará o body com a mensagem 'Hello World'."
  [request]
  {:status 200 ;Esse será o status de retorno.
   :body (str "Hello World!" (get-in request [:query-params :name] "Retorno padrão caso nenhum parâmetro 'name' seja enviado!"))
   }) ;Esse será o retorno do end-point.

(def routes (route/expand-routes #{["/hello-world" :get funcao-hello :route-name :hello-world]}))

(def service-map
  {::http/routes routes
   ::http/port 9999 ;Essa será a porta padrão da aplicação.
   ::http/type :jetty  ;Esse será o servidor utilizado.
   ::http/join? false}) ;Em ambiente de dev, essa configuração é importante para o Clojure não travar a thread ao iniciar o processo, assim, podemos continuar testando a aplicação. Em ambiente de produção, não precisamos dessa configuração, pois se a thread morrer, o serviço deverá morrer também.

(http/start (http/create-server service-map))
(println "O servidor HTTP foi inicializado!")