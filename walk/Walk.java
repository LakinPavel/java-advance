package info.kgeorgiy.ja.Laskin_Pavel.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Walk {

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Incorrect number of arguments");
        } else if (args[0] == null || args[1] == null) {
            System.out.println("Incorrect arguments");
        } else {
            String fileNameInput = args[0];
            String fileNameOutput = args[1];
            try {
                Path in = Paths.get(fileNameInput);
                Path out = Paths.get(fileNameOutput);
                Path pathParentOutput = out.getParent();
                if (pathParentOutput != null) {
                    try {
                        if (!Files.exists(pathParentOutput)) {
                            Files.createDirectories(pathParentOutput);
                        }
                    } catch (IOException e) {
                        System.err.println("Cant create directories " + e.getMessage());
                    }
                } else {
                    System.out.println("There is no parent");
                }

                try (BufferedReader input = new BufferedReader(
                        new InputStreamReader(new FileInputStream(in.toFile()), "UTF-8"));
                     BufferedWriter outputFile = new BufferedWriter(
                             new OutputStreamWriter(new FileOutputStream(out.toFile()), "UTF-8"));) {


                    while (input.ready()) {
                        try {
                            String filePath = input.readLine();
                            try {
                                Path path = Paths.get(filePath);
                                try (FileInputStream currentFile = new FileInputStream(path.toFile())) {
                                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                                    byte[] partOfFile = new byte[1024];
                                    int flagOfEnd = currentFile.read(partOfFile);
                                    while (flagOfEnd != -1) {
                                        md.update(partOfFile, 0, flagOfEnd);
                                        flagOfEnd = currentFile.read(partOfFile);
                                    }
                                    byte[] hashByte = md.digest();
                                    StringBuilder ansHash = new StringBuilder();
                                    for (int i = 0; i < Math.min(hashByte.length, 8); i++) {
                                        ansHash.append(String.format("%02x", hashByte[i]));
                                    }
                                    outputFile.write(ansHash + " " + filePath + "\n");
                                } catch (NoSuchAlgorithmException e) {
                                    System.out.println("The SHA-256 algorithm is not supported " + e.getMessage());
                                } catch (FileNotFoundException e) {
                                    outputFile.write("0000000000000000 " + filePath + "\n");
                                    System.out.println("File not found " + e.getMessage());
                                } catch (SecurityException e) {
                                    outputFile.write("0000000000000000 " + filePath + "\n");
                                    System.out.println("There is no access to the file " + e.getMessage());
                                }
                            } catch (InvalidPathException e) {
                                outputFile.write("0000000000000000 " + filePath + "\n");
                                System.out.println("The path string cannot be converted to a Path " + e.getMessage());
                            }
                        } catch (IOException e) {
                            System.out.println("Cant read filePath " + e.getMessage());
                        }
                    }


                } catch (FileNotFoundException e) {
                    System.err.println("File not found " + e.getMessage());
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Named charset is not supported " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("Error IOE" + e.getMessage());
                } catch (SecurityException e) {
                    System.out.println("There is no access to the file " + e.getMessage());
                }
            } catch (InvalidPathException e) {
                System.out.println("The path string cannot be converted to a Path " + e.getMessage());
            }
        }
    }
}
