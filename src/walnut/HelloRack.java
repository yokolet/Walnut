package walnut;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

/**
 * Servlet implementation class HelloRack
 */
@WebServlet("/HelloRack")
public class HelloRack extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private List<String> gemPaths;
	private ServletConfig config;
    private ScriptingContainer container;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HelloRack() {
        super();
        gemPaths = new ArrayList<String>();
        container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    }
    
    private void addGemPaths(String gem_path) {
        File gem_dir = new File(gem_path);
        File[] gems = gem_dir.listFiles();
        for (File gem : gems) {
            String path = gem + "/lib";
            gemPaths.add(path);
        }
    }
    
    public void init(ServletConfig config) {
        this.config = config;
        String path = config.getServletContext().getRealPath("/WEB-INF/lib/jruby/1.8/gems");
        addGemPaths(path);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    processHttpRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processHttpRequest(request, response);
	}

	private void processHttpRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    container.setLoadPaths(gemPaths);
        String class_def = 
                "require 'rubygems'\n" +
                "require 'sinatra/base'\n" +
                "class MyApp < Sinatra::Base\n" +
                "  get '/' do\n" +
                "    'Hello from Sinatra over JRuby-Rack'\n" +
                "  end\n" +
                "end\n" +
                "MyApp.new";
        Object myApp = container.runScriptlet(class_def);
        container.put("rack_app", myApp);
        String creates_handler = 
                "require 'jruby-rack'\n" +
                "require 'rack/handler/servlet'\n" +
                "Rack::Handler::Servlet.new rack_app";
        Object handler = container.runScriptlet(creates_handler);
        container.put("handler", handler);
        container.put("request", request);
        container.put("config", config);
        String calls_app = 
                "request.instance_variable_set(:@context, config.getServletContext)\n" +
                "class << request\n" +
                "  def to_io\n" +
                "    self.getInputStream.to_io\n" +
                "  end\n" +
                "  def getScriptName\n"+
                "    self.getPathTranslated\n" +
                "  end\n" +
                "  def context\n" +
                "    @context\n" +
                "  end\n" +
                "end\n" +
                "handler.call(request)";
        Object rack_response = container.runScriptlet(calls_app);
        String body = (String)container.callMethod(rack_response, "getBody", String.class);
        response.getWriter().print(body);
        container.clear();
	}
}
