package lzr.servlet;

import lzr.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {
    private final Properties contextConfig = new Properties();
    private final List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception , Detail : " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replace(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(uri)) {
            resp.getWriter().write("404 not found");
            return;
        }
        Method method = (Method) this.handlerMapping.get(uri);
        Map<String, String[]> params = req.getParameterMap();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Map<String, String[]> parameterMap = req.getParameterMap();
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
            } else {
                Annotation[][] pa = method.getParameterAnnotations();
                Annotation[] annotations = pa[i];
                for (Annotation a : annotations) {
                    if (a instanceof RequestParam) {
                        String pName = ((RequestParam) a).value();
                        if (!"".equals(pName.trim())) {
                            Object value = Arrays.toString(parameterMap.get(pName))
                                    .replaceAll("\\[|\\]", "")
                                    .replaceAll("\\s", ",");

                            System.out.println(value);
                            if (parameterType == Integer.class) {
                                value = Integer.valueOf((String) value);
                            }
                            paramValues[i] = value;
                        }
                    }
                }
            }
        }

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), paramValues);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //doScanner(contextConfig.getProperty("scanPackage"));
        doScanner("lzr.demo");

        doInstance();

        doAutoWired();

        initHandlerMapping();

        System.out.println("init end");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        ioc.forEach((k, v) -> {
            Class<?> clazz = v.getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) return;
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                baseUrl = clazz.getAnnotation(RequestMapping.class).value();
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(RequestMapping.class)) continue;
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
            }
        });
    }

    private void doAutoWired() {
        if (ioc.isEmpty()) return;
        ioc.forEach((k, v) -> {
            Field[] fields = v.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(AutoWired.class)) continue;
                String beanName = toLowerFirstCase(field.getType().getSimpleName());
                field.setAccessible(true);
                try {
                    field.set(v, ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void doInstance() {
        if (classNames.isEmpty()) return;
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Service.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doLoadConfig(String contextConfigLocation) {
        System.out.println(contextConfigLocation);
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);) {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : Objects.requireNonNull(classDir.listFiles())) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) continue;
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                classNames.add(className);
            }
        }

    }
}
