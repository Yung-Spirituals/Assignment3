package no.ntnu.datakomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread{
  private final Socket clientSocket;

  public  ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {

    try{
      InputStreamReader reader = new InputStreamReader(clientSocket.getInputStream());
      BufferedReader bufferedReader = new BufferedReader(reader);
      PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(),true);

      String clientInput = bufferedReader.readLine();
      System.out.println("Client sent: " + clientInput);
      String[] parts = clientInput.split(" ");

      String response = "Sorry, no math questions...";


      writer.println(response);

      clientSocket.close();
    }catch (IOException e){
      e.printStackTrace();
    }
  }
}