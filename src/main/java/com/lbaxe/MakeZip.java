package com.lbaxe;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.crypto.digest.DigestUtil;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * Hello world!
 */
public class MakeZip {
    private static Map<String,String> zipPasswordMap = new HashMap<>();
    private static Pattern pattern = Pattern.compile("^(makezip|exit$)([\\t|\\s]+)?([a-z|A-Z|0-9]+)?");
    private static Pattern winPath = Pattern.compile("^(cd)([\\t|\\s]+){1}((?:[a-zA-Z]:\\\\|\\\\\\\\[^\\\\\\/:*?\"<>|\\r\\n]+\\\\[^\\\\\\/:*?\"<>|\\r\\n]*)(?:[^\\\\\\/:*?\"<>|\\r\\n]+\\\\)*[^\\\\\\/:*?\"<>|\\r\\n]*)$");
    private static Pattern linuxPath = Pattern.compile("^(cd)([\\t|\\s]+){1}((\\/[^\\/]+)*\\/?[^\\/]*)$");
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("识别指令：");
        System.out.println("1. makezip [password]");
        System.out.println("2. exit");
        System.out.println("3. cd [path]");
        while(true){
            String workDir = System.getProperty("user.dir");
            System.out.println("当前压缩目录["+workDir+"]，请输入指令：");
            String line =  scanner.nextLine();
            if(line == null){
                continue;
            }
            Matcher matcher = pattern.matcher(line);
            boolean match = matcher.matches();
            if(match){
                String cmd = matcher.group(1);
                if ("makezip".equals(cmd)){
                    String password = matcher.groupCount() >= 3 ?  matcher.group(3) : null;
                    generateZip(workDir,password);
                      /*zipPasswordMap.entrySet().stream().forEach(e->{
                        System.out.println(e.getKey() + "_" + e.getValue());
                        });*/
                    System.out.println("当前压缩目录["+workDir+"]，压缩完成！");
                } else if("exit".equals(cmd)){
                    break;
                }
            }else{
                String osName = System.getProperty("os.name");
                if(osName.toLowerCase().startsWith("windows")){
                    matcher = winPath.matcher(line);
                    match = matcher.matches();
                    if(match){
                        String cmd = matcher.group(1);
                        if ("cd".equals(cmd)){
                            System.setProperty("user.dir",matcher.group(3));
                        }
                    }
                }else {
                    matcher = linuxPath.matcher(line);
                    match = matcher.matches();
                    if(match){
                        String cmd = matcher.group(1);
                        if ("cd".equals(cmd)){
                            System.setProperty("user.dir",matcher.group(3));
                        }
                    }
                }
            }

        }
    }
    public static void generateZip(String path,String password) throws IOException{
        generateZip(new File(path),password);
    }
    public static void generateZip(File file,String password) throws IOException {
        if(file == null || !file.exists()){
            return;
        }

        if(file.isDirectory()){
            File[] subFiles = file.listFiles((dir, name) -> !name.endsWith(".zip") && !name.startsWith("MakeZip") && !name.endsWith("jar"));
            if (subFiles == null || subFiles.length <= 0) {
                return;
            }
            for (File sub : subFiles) {
                generateZip(sub,password);
            }
        }else {
            String prefixName = file.getName().substring(0,file.getName().lastIndexOf("."));
            String basePath = file.getAbsolutePath().substring(0,file.getAbsolutePath().lastIndexOf(file.getName()));
            String targetZipFileName = prefixName + ".zip";
            String targetZipFile = basePath + targetZipFileName;
            File target = new File(targetZipFile);
            if(target.exists()){
                target.delete();
                System.out.print("删除旧压缩文件，");
            }
            System.out.println("压缩["+file.getName()+"]");
            //计算文件md5值
            String md5 = DigestUtil.md5Hex(file);
            try {
                ZipFile zipFile = null;
                ZipParameters parameters = new ZipParameters();
                parameters.setCompressionMethod(CompressionMethod.DEFLATE); // 设置压缩方法
                parameters.setCompressionLevel(CompressionLevel.NORMAL); // 设置压缩级别
                if(password == null || password.equals("")){
                    parameters.setEncryptFiles(false);
                    zipFile = new ZipFile(targetZipFile);
                }else {
                    parameters.setEncryptFiles(true); // 启用文件加密
                    parameters.setEncryptionMethod(EncryptionMethod.AES); // 使用 AES 加密
                    parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256); // 使用 AES-256
                    //生成加密zip的密码
                    //String password = SecureUtil.sha1().digestHex(md5+"makezip@luban").substring(0,4);
                    //生成加密的zip文件,密码规则lbaxe + md5加密结果
                    zipFile = new ZipFile(targetZipFile,password.toCharArray());
                }
                zipFile.addFile(file, parameters);
                //添加README.txt
                ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos));
                bw.write(file.getName() + "的MD5值：" + md5);
                bw.newLine();
                bw.write("鲁班Java技术10群：427304576");
                bw.flush();
                parameters.setFileNameInZip("README.txt");
                zipFile.addStream(new ByteArrayInputStream(baos.toByteArray()),parameters);
                zipFile.close();
                baos.close();
                zipPasswordMap.put(targetZipFileName,password);
            } catch (Exception e) {
                System.out.println(targetZipFileName);
                e.printStackTrace();
            }
        }
    }
}
