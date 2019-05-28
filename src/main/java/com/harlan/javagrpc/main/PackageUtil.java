package com.harlan.javagrpc.main;

import com.harlan.javagrpc.converter.RemoteAccessEnabled;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackageUtil {

	public static void main(String[] args) throws IOException {

		String packageName = "com.harlan.javagrpc.service";
		String protoDir = "src/main/proto/";
		Files.createDirectories(Paths.get(protoDir));
		
		List<Class<?>> classes = ClassGrabberUtil.getClasses(packageName, RemoteAccessEnabled.class);
		for (Class<?> clazz : classes) {

			try {
				String name = clazz.getSimpleName();
				StringBuffer sb = new StringBuffer(2048);
				sb.append("syntax = \"proto3\";\r\n");
				sb.append("\r\n");
				sb.append("import \"google/protobuf/empty.proto\";\r\n");
				sb.append("\r\n");
				sb.append("option java_multiple_files = true;\r\n");
				sb.append("option java_package = \"" + clazz.getPackage().getName() + ".protobuf\";\r\n");
				sb.append("option java_outer_classname = \"" + name + "Proto\";\r\n");
				sb.append("\r\n");
				sb.append("package " + name + ";\r\n");
				sb.append("\r\n");
				sb.append("service " + name + " {\r\n");
				
				//服务
				Method[] methods = clazz.getDeclaredMethods(); // excludes inherited methods
				for (Method method : methods) {
					if (Modifier.isPrivate(method.getModifiers())) {
						continue;
					}
					sb.append("\t" + "rpc ");
					sb.append(method.getName() + " ");
					
					// parameter type
					String methodParameterType = "google.protobuf.Empty";
					if (method.getParameterCount() > 0) {
						methodParameterType = capitalizeFirstChar(method.getName()) + "MessageIn";
					}
					
					sb.append("(" + methodParameterType + ") returns ");
					
					// return type
					String methodReturnType = "google.protobuf.Empty";
					if (!method.getReturnType().equals(Void.TYPE)) {
						methodReturnType = capitalizeFirstChar(method.getName()) + "MessageOut";
					}
					
					sb.append("(" + methodReturnType + ") {};\r\n");
				}
				sb.append("}\r\n");
				sb.append("\r\n");
				
				//message
				for (Method method : methods) {
					if (Modifier.isPrivate(method.getModifiers())) {
						continue;
					}
					
					// has any parameter?
					if (method.getParameterCount() > 0) {
						
						sb.append("message " + capitalizeFirstChar(method.getName()) + "MessageIn {\r\n");
						
						Class<?>[] reqParam = method.getParameterTypes();
						if(reqParam.length == 1) {
							for (Class<?> cl : reqParam) {
								Field[] fields = cl.getDeclaredFields();
								int i = 1;
								for (Field field : fields) {
									handleField(sb, field, i);
									i++;
								}
							}
						} else if (reqParam.length > 1) {
							List<String> nameList = new ArrayList<>();
							for (Class<?> cl : reqParam) {
								nameList.add(cl.getSimpleName());
								sb.append("\t message " + cl.getSimpleName() + " {\r\n");
								Field[] fields = cl.getDeclaredFields();
								int i = 1;
								for (Field field : fields) {
									handleField(sb, field,i);
									i++;
								}
								sb.append("\t}\r\n");
							}
							for (int i = 0; i <= nameList.size() - 1; i++) {
								sb.append("\t repeated " + nameList.get(i) + " " + nameList.get(i) + " = " + (i + 1) + ";\r\n");
							}
						}
						sb.append("}\r\n");
					}
					
					// has return type?
					if (!method.getReturnType().equals(Void.TYPE)) {
						
						sb.append("message " + capitalizeFirstChar(method.getName()) + "MessageOut {\r\n");
						
						Class<?> resClazz = method.getReturnType();
						Field[] fields = resClazz.getDeclaredFields();
						int i = 1;
						for (Field field : fields) {
							handleField(sb, field,i);
							i++;
						}
						sb.append("}\r\n");
					}
				}

				FileWriter writer = new FileWriter(protoDir + name + ".proto");
				writer.write(sb.toString());
				writer.close();
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	private static String capitalizeFirstChar(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * 判断是否为自定义对象类型
	 * @param clz
	 * @return
	 */
	private static boolean isJavaClass(Class<?> clz) {
		return clz != null && clz.getClassLoader() == null;
	}
	
	private static void handleField(StringBuffer sb, Field field, Integer i) {
		if (field.getType() == Map.class) {
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			handleGeneric(sb, field, pt.getActualTypeArguments()[0].getTypeName(), i);
			handleGeneric(sb, field, pt.getActualTypeArguments()[1].getTypeName(), i);
			sb.append("\tmap<" + getGenericByTypeName(pt.getActualTypeArguments()[0].getTypeName())+ ", " + 
						getGenericByTypeName(pt.getActualTypeArguments()[1].getTypeName()) + 
					"> " + field.getName() + " = "+ i + ";\r\n");
		}
		else if(field.getType() == List.class) {
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			try {
				Class<?> clazz = Class.forName(pt.getActualTypeArguments()[0].getTypeName());
				if(isJavaClass(clazz)) {
					sb.append("\trepeated " + handleFieldType(clazz.getName()) + " " +  field.getName() + " = " + i + ";\r\n");
					return;
				}
				sb.append("\tmessage " + clazz.getSimpleName() + " { \r\n");
				Field[] fields = clazz.getDeclaredFields();
				int j = 1;
				for(Field f : fields) {
					handleField(sb, f, j);
					j++;
				}
				sb.append("\t}\r\n");
				sb.append("\trepeated " + clazz.getSimpleName() + " " + field.getName() + " = " + i + ";\r\n");
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else if(isJavaClass(field.getType())) {
			sb.append("\t" + handleFieldType(field.getType().getName()) + " " + field.getName() + " = " + i + ";\r\n");
			return;
		}else {
			sb.append("\tmessage " + field.getType().getSimpleName() + " { \r\n");
			int j = 1;
			try {
				Class<?> cl = Class.forName(field.getType().getName());
				Field[] fields = cl.getDeclaredFields();
				for(Field f : fields) {
					handleField(sb, f, j);
					j++;
				}
				sb.append("\t}\r\n");
				sb.append("\trepeated " + field.getType().getSimpleName() + " " +field.getName() + " = " + i +";\r\n");
//				System.out.println(field.getType().getName());
//				System.out.println("自定义对象");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static String getGenericByTypeName(String typeName) {
		try {
			Class<?> clazz = Class.forName(typeName);
			if (isJavaClass(clazz)) {
				return handleFieldType(typeName);
			} else {
				return clazz.getSimpleName();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void handleGeneric(StringBuffer sb,Field field,String typeName,int i) {
		try {
			Class<?> clazz = Class.forName(typeName);
			if(isJavaClass(clazz)) {
				//sb.append("\trepeated " + clazz.getSimpleName() + " " +  field.getName() + " = " + i + ";\r\n");
				return;
			}
			sb.append("\tmessage " + clazz.getSimpleName() + " { \r\n");
			Field[] fields = clazz.getDeclaredFields();
			int j = 1;
			for(Field f : fields) {
				handleField(sb, f, j);
				j++;
			}
			sb.append("\t}\r\n");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private static String handleFieldType(String typeName) {
		String returnName = "";
		switch (typeName) {
		case "int":
		case "java.lang.Integer":
			returnName = "int32";
			break;
		case "long":
		case "java.lang.Long":
			returnName = "int64";
			break;
		case "java.lang.String":
			returnName = "string";
			break;
		case "double":
		case "java.lang.Double":
			returnName = "double";
			break;
		case "float":
		case "java.lang.Float":
			returnName = "float";
			break;
		case "boolean":
		case "java.lang.Boolean":
			returnName = "bool";
			break;
		case "byte":
		case "java.lang.Byte":
			returnName = "byte";
			break;
		default:
			break;
		}
		return returnName;
	}
	
}
