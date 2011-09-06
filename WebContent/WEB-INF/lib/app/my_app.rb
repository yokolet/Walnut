require 'sinatra/base'

class MyApp < Sinatra::Base
  get '/' do
    'Hello from Sinatra App over JRuby-Rack!'
  end
end
