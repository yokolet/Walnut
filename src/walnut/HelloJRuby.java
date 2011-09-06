package walnut;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

/**
 * Servlet implementation class HelloJRuby
 */
@WebServlet("/HelloJRuby")
public class HelloJRuby extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private List<String> gemPaths;
	private ServletConfig config;
	private ScriptingContainer container;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HelloJRuby() {
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
                "    'Hello from Sinatra'\n" +
                "  end\n" +
                "end\n" +
                "MyApp.new";
        Object myApp = container.runScriptlet(class_def);
        Map<String, String> minimal_env = new ConcurrentHashMap<String, String>();
        minimal_env.put("PATH_INFO", "/");
        minimal_env.put("REQUEST_METHOD", "GET");
        minimal_env.put("rack.input", "");
        List rack_response = (List)container.callMethod(myApp, "call", minimal_env, List.class);
        response.getWriter().print(rack_response.get(2));
        container.clear();
    }
}
