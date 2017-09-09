package com.latewind.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.latewind.annotation.Controller;
import com.latewind.annotation.Qualifier;
import com.latewind.annotation.RequestMapping;
import com.latewind.annotation.Service;
import com.latewind.config.GlobalConfig;

/**
 * Servlet implementation class DispatchServlet
 */
@WebServlet("/DispatchServlet")
public class DispatchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private GlobalConfig globalConfig;
	private List<String> classes = Lists.newLinkedList();
	private HashMap<String, Object> instanceMap = Maps.newHashMap();
	private HashMap<String, Object> hanlderMap = Maps.newHashMap();
	Logger logger = Logger.getLogger(getClass());

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DispatchServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		logger.info("get");

		String url = request.getRequestURI();
		String context = request.getContextPath();
		String path = url.replace(context, "");
		Object instance = instanceMap.get(hanlderMap.get(path));
		Object result = null;
		Method[] methods = instance.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].isAnnotationPresent(RequestMapping.class)) {
				RequestMapping rm = (RequestMapping) methods[i].getAnnotation(RequestMapping.class);
				String value = rm.value();
				if (path.equals(value)) {
					try {
						result = methods[i].invoke(instance);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}
		request.getRequestDispatcher("/WEB-INF/views/" + result.toString() + ".jsp").forward(request, response);
	}

	private void handlerMapping() {
		for (Entry<String, Object> entry : instanceMap.entrySet()) {
			Object instance = entry.getValue();
			if (instance.getClass().isAnnotationPresent(Controller.class)) {
				Method[] methods = instance.getClass().getMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isAnnotationPresent(RequestMapping.class)) {
						RequestMapping rm = (RequestMapping) methods[i].getAnnotation(RequestMapping.class);
						String value = rm.value();
						hanlderMap.put(value, entry.getValue().getClass().getName());

					}
				}
			}

		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		loadConfig();
		scanPackage(globalConfig.getBasePackage());
		filterAndInstance();
		ioc();
		handlerMapping();
	}

	private void loadConfig() {
		Reader reader = null;
		try {
			reader = new InputStreamReader(GlobalConfig.class.getResourceAsStream("/config.json"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Gson gson = new GsonBuilder().create();
		globalConfig = gson.fromJson(reader, GlobalConfig.class);
	}

	private void scanPackage(String packageName) {
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replace('.', '/'));
		String path = url.getPath();
		File file = new File(path);

		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				scanPackage(packageName + "." + files[i].getName());
			} else {
				logger.info(packageName + "." + files[i].getName());
				classes.add(packageName + "." + files[i].getName());
			}
		}
	}

	private void filterAndInstance() {
		for (String clazzName : classes) {
			try {
				Class clazz = Class.forName(clazzName.replace(".class", ""));
				if (clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Service.class)) {
					Object instance = clazz.newInstance();
					logger.info(instance.getClass().getName());
					instanceMap.put(instance.getClass().getName(), instance);
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void ioc() {
		for (Entry<String, Object> entry : instanceMap.entrySet()) {
			Object instance = entry.getValue();
			Field[] fields = instance.getClass().getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].isAnnotationPresent(Qualifier.class)) {
					Class<?> type = fields[i].getType();
					logger.info("Type" + type.getName());
					fields[i].setAccessible(true);
					try {
						fields[i].set(entry.getValue(), instanceMap.get(type.getName()));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}
