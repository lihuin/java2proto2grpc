package com.harlan.javagrpc.main;

import com.harlan.javagrpc.converter.RemoteAccessEnabled;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

public class PackageUtil2 {
	
	private static final Map<String,TreeMap<Integer,String>> map = new HashMap<>();
	
	public static void main(String[] args) throws IOException {

		final String packageName = "com.harlan.javagrpc.service.contract";
		final String protoDir = "src/main/proto/";
		Files.createDirectories(Paths.get(protoDir));
		
		List<Class<?>> classes = ClassGrabberUtil.getClasses(packageName, RemoteAccessEnabled.class);
		for (Class<?> clazz : classes) {

			try {
				String name = clazz.getSimpleName();
				StringBuffer sb = new StringBuffer(2048);
				sb.append("syntax = \"proto3\";\r\n");
				sb.append("\r\n");
				sb.append("import \"google/protobuf/empty.proto\";\r\n"); // for empty return type and/or parameters
				sb.append("import \"google/protobuf/timestamp.proto\";\r\n"); // for time representation from LocalDateTime, LocalDate and LocalTime classes
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
				
				//messages
				for (Method method : methods) {
					if (Modifier.isPrivate(method.getModifiers())) {
						continue;
					}
					
					// has any parameter?
					if (method.getParameterCount() > 0) {
					
						sb.append("message " + capitalizeFirstChar(method.getName()) + "MessageIn {\r\n");
					
						Parameter[] parameters = method.getParameters();
						if (parameters.length == 1) {
							Parameter parameter = parameters[0];
							Class<?> cl = parameter.getType();
							String paramType = getProtobufFieldType(cl.getSimpleName());
							if ("".equals(paramType)) {
								paramType = cl.getSimpleName();
							}
							String paramName = lowerCaseFirstChar(cl.getSimpleName());
							sb.append("\t" + paramType + " " + paramName + " = 1;\r\n");
							
							TreeMap<Integer,String> tm = new TreeMap<Integer,String>();
							map.put(cl.getName(), tm);
							// process fields
							if (cl.isEnum()) {
								handleEnum(cl);
							} else {
								Field[] fields = cl.getDeclaredFields();
								int i = 1;
								for (Field field : fields) {
									handleField(sb, field, i, tm);
									i++;
								}
							}
						}
						else if (parameters.length > 1) {
	//						List<String> nameList = new ArrayList<>();
							int count = 0;
							for (Parameter parameter : parameters) {
								Class<?> cl = parameter.getType();
	//							nameList.add(cl.getSimpleName());
								String paramType = getProtobufFieldType(cl.getSimpleName());
								if ("".equals(paramType)) {
									paramType = cl.getSimpleName();
								}
								String paramName = lowerCaseFirstChar(cl.getSimpleName());
								sb.append("\t" + paramType + " " + paramName + " = " + (++count) + ";\r\n");
								
								TreeMap<Integer, String> tm = new TreeMap<Integer, String>();
								map.put(cl.getName(), tm);
								// process fields
								if (cl.isEnum()) {
									handleEnum(cl);
								} else {
									Field[] fields = cl.getDeclaredFields();
									int i = 1;
									for (Field field : fields) {
										handleField(sb, field, i, tm);
										i++;
									}
								}
	//							sb.append("\t}\r\n");
							}
	//						for (int i = 0; i <= nameList.size() - 1; i++) {
	//							sb.append("\t repeated " + nameList.get(i) + " " + nameList.get(i) + " = " + (i + 1) + ";\r\n");
	//						}
						}
						
						sb.append("}\r\n");
					}
					
					// has return type?
					if (!method.getReturnType().equals(Void.TYPE)) {
					
						sb.append("message " + capitalizeFirstChar(method.getName()) + "MessageOut {\r\n");
						
						Class<?> resClazz = method.getReturnType();
	//					sb.append("\t" + resClazz.getSimpleName() + " " + resClazz.getSimpleName() + " = 1;\r\n");
						if (isJavaClass(resClazz)) {
							String returnType = getProtobufFieldType(resClazz.getSimpleName());
							String returnName = lowerCaseFirstChar(resClazz.getSimpleName());
							sb.append("\t" + returnType + " " + returnName + " = 1 ;\r\n");
						}
						else {
							String returnType = resClazz.getSimpleName();
							String returnName = lowerCaseFirstChar(resClazz.getSimpleName());
							sb.append("\t" + returnType + " " + returnName + " = 1;\r\n");
							
							TreeMap<Integer,String> tm = new TreeMap<Integer,String>();
							map.put(resClazz.getName(), tm);
							
							// process fields
							if (resClazz.isEnum()) {
								handleEnum(resClazz);
							} else {
								Field[] fields = resClazz.getDeclaredFields();
								int i = 1;
								for (Field field : fields) {
									handleField(sb, field, i, tm);
									i++;
								}
							}
						}
						
						sb.append("}\r\n");
					}
				}
				sb.append(Map2StringBuffer(map));
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

	private static String lowerCaseFirstChar(String s) {
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}
	
	private static StringBuffer Map2StringBuffer(Map<String, TreeMap<Integer, String>> map) {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, TreeMap<Integer, String>> entry : map.entrySet()) {
			String className = entry.getKey();
			TreeMap<Integer, String> fieldMap = entry.getValue();
			try {
				Class<?> clazz = Class.forName(className);
				if (clazz.isEnum()) {
					// NOTE: do not generate enum types since we treat them as string
//					sb.append("enum " + clazz.getSimpleName() + " {\r\n");
//					for (Integer key : fieldMap.keySet()) {
//						String field = fieldMap.get(key);
//						sb.append(field);
//					}
//					sb.append("}\r\n");
				} else {
					sb.append("message " + clazz.getSimpleName() + " {\r\n");
					for (Integer key : fieldMap.keySet()) {
						String field = fieldMap.get(key);
						sb.append(field);
					}
					sb.append("}\r\n");
				}
			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
			}
		}
		return sb;
	}
	
	/**
	 * 判断是否为自定义对象类型
	 * @param clz
	 * @return
	 */
	private static boolean isJavaClass(Class<?> clz) {
		return clz != null && !"".equals(getProtobufFieldType(clz.getName()));
		//return clz != null && clz.getClassLoader() == null;
	}
	
	private static void handleEnum(Class<?> cl) {
		// NOTE: currently Enum types are not being generated since they are defined as string
//		TreeMap<Integer, String> fieldsMap = map.get(cl.getName());
//		// if Enum was already processed then do nothing
//		if (fieldsMap != null && !fieldsMap.isEmpty()) {
//			return;
//		}
//		int i = 0;
//		for(Object e : cl.getEnumConstants()){
//			String enumFieldName = "\t" + e.toString() + " = " + i + ";\r\n";
//			fieldsMap.put(i, enumFieldName);
//			++i;
//		}
	}

	private static void handleField(StringBuffer sb, Field field, Integer i, TreeMap<Integer,String> tm) {
		if (field.getType() == Map.class) {
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			handleGeneric(sb, field, pt.getActualTypeArguments()[0].getTypeName(), i, tm);
			handleGeneric(sb, field, pt.getActualTypeArguments()[1].getTypeName(), i, tm);
//			sb.append("\tmap<" + getGenericByTypeName(pt.getActualTypeArguments()[0].getTypeName())+ ", " + 
//						getGenericByTypeName(pt.getActualTypeArguments()[1].getTypeName()) + "> " + field.getName() + " = "+ i + ";\r\n");
			tm.put(i, "\tmap<" + getGenericByTypeName(pt.getActualTypeArguments()[0].getTypeName())+ ", " + 
						getGenericByTypeName(pt.getActualTypeArguments()[1].getTypeName()) + "> " + field.getName() + " = "+ i + ";\r\n");
		}
		else if (field.getType() == List.class || field.getType() == Set.class || field.getType() == Queue.class) {
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			try {
				Class<?> clazz = Class.forName(pt.getActualTypeArguments()[0].getTypeName());
				if(isJavaClass(clazz)) {
					sb.append("\trepeated " + getProtobufFieldType(clazz.getName()) + " " +  field.getName() + " = " + i + ";\r\n");
					return;
				}
//				sb.append("\tmessage " + clazz.getSimpleName() + " { \r\n");
				if (!map.containsKey(clazz.getName())) {
					TreeMap<Integer, String> listTm = new TreeMap<Integer, String>();
					map.put(clazz.getName(), listTm);
					if (clazz.isEnum()) {
						handleEnum(clazz);
					} else {
						Field[] fields = clazz.getDeclaredFields();
						int j = 1;
						for (Field f : fields) {
							handleField(sb, f, j, listTm);
							j++;
						}
					}
				}
//				sb.append("\t}\r\n");
//				sb.append("\trepeated " + clazz.getSimpleName() + " " + field.getName() + " = " + i + ";\r\n");
				tm.put(i, "\trepeated " + clazz.getSimpleName() + " " + field.getName() + " = " + i + ";\r\n");
			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
			}
		}
		else if (isJavaClass(field.getType())) {
			//sb.append("\t" + handleFieldType(field.getType().getName()) + " " + field.getName() + " = " + i + ";\r\n");
			tm.put(i, "\t" + getProtobufFieldType(field.getType().getName()) + " " + field.getName() + " = " + i + ";\r\n");
			return;
		}
		else if (field.isEnumConstant()) {
			handleEnum(field.getType());
		}
		else {
//			sb.append("\tmessage " + field.getType().getSimpleName() + " { \r\n");
			int j = 1;
			try {
				Class<?> cl = Class.forName(field.getType().getName());
				if(!map.containsKey(cl.getName())) {
					TreeMap<Integer, String> ObjTm = new TreeMap<Integer, String>();
					map.put(cl.getName(), ObjTm);
					if (cl.isEnum()) {
						handleEnum(cl);
					} else {
						Field[] fields = cl.getDeclaredFields();
						for(Field f : fields) {
							handleField(sb, f, j,ObjTm);
							j++;
						}
					}
//					sb.append("\t}\r\n");
				}
//				sb.append("\t" + field.getType().getSimpleName() + " " +field.getName() + " = " + i +";\r\n");
				tm.put(i, "\t" + field.getType().getSimpleName() + " " +field.getName() + " = " + i +";\r\n");
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
				return getProtobufFieldType(typeName);
			} else {
				return clazz.getSimpleName();
			}
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		}
		return null;
	}
	
	private static void handleGeneric(StringBuffer sb, Field field ,String typeName, int i, TreeMap<Integer,String> tm) {
		try {
			Class<?> clazz = Class.forName(typeName);
			if(isJavaClass(clazz)) {
				//sb.append("\trepeated " + clazz.getSimpleName() + " " +  field.getName() + " = " + i + ";\r\n");
				return;
			}
			if(!map.containsKey(clazz.getName())) {
				TreeMap<Integer, String> ObjTm = new TreeMap<Integer, String>();
				map.put(clazz.getName(), ObjTm);
				if (clazz.isEnum()) {
					handleEnum(clazz);
				} else {
					Field[] fields = clazz.getDeclaredFields();
					int j = 1;
					for(Field f : fields) {
						handleField(sb, f, j,ObjTm);
						j++;
					}
				}
			}
//			sb.append("\tmessage " + clazz.getSimpleName() + " { \r\n");
			
//			sb.append("\t}\r\n");
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private static String getProtobufFieldType(String typeName) {
		switch (typeName) {
		case "int":
		case "java.lang.Integer":
			return "sint32";
		case "long":
		case "java.lang.Long":
			return "sint64";
		case "java.lang.String":
			return "string";
		case "double":
		case "java.lang.Double":
			return "double";
		case "float":
		case "java.lang.Float":
			return "float";
		case "boolean":
		case "java.lang.Boolean":
			return "bool";
		case "byte":
		case "java.lang.Byte":
			return "byte";
		case "java.time.LocalDateTime":
		case "java.time.LocalDate":
		case "java.time.LocalTime":
		case "java.util.Date":
			return "Timestamp";
		default:
			break;
		}
		
		try {
			Class<?> clazz = Class.forName(typeName);
			// Enum is treated as string
			if (clazz.isEnum()) {
				return "string";
			}
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		}
		
		return "";
	}

}
