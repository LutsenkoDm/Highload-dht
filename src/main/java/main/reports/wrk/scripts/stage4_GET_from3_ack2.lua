counter = 0

request = function()
   path = "/v0/entity?id=k" .. counter .. "&from=3&ack=2"
   wrk.method = "GET"
   counter = counter + 1
   return wrk.format(nil, path)
end
