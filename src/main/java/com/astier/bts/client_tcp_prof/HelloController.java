package com.astier.bts.client_tcp_prof;

import com.astier.bts.totp.Totp;
import com.astier.bts.client_tcp_prof.exceptions.DiagnosticException;
import com.astier.bts.client_tcp_prof.modeles.Ipv4;
import com.astier.bts.client_tcp_prof.multicast_diffusion.MulticastDiffusion;
import com.astier.bts.client_tcp_prof.outils.ScanInterfaces;
import com.astier.bts.client_tcp_prof.tcp.TCPClient;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;

import static java.lang.Integer.parseInt;
import static javafx.scene.paint.Color.*;

public class HelloController implements Initializable {
    public Button button;
    public Button connecter;
    public Button deconnecter;
    public TextField TextFieldIP;
    public TextField TextFieldPort;
    public TextField TextFieldRequette;
    public Circle voyant;
    public TextArea TextAreaReponses;
    public ChoiceBox choiceBoxInterfaces;
    static public TCPClient tcp;
    static boolean enRun = false;
    String adresse,port;
    static MulticastDiffusion multicastDiffusion;
    static String interfaceName;
    private final String CODE = "ZPJMQKNMQXNCOEFNNEF";


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            verifierTotp();
            getInterfaces();

            connecter.setOnMouseClicked(e -> this.connecter());
            deconnecter.setOnMouseClicked(_ -> this.deconnecter());
            button.setOnMouseClicked(_ -> this.envoyer());
            TextFieldRequette.setOnAction(_ -> this.envoyer());
            choiceBoxInterfaces.getSelectionModel().selectedItemProperty().addListener(
                    (observable, ancienneValeur, nouvelleValeur) -> {
                Ipv4 monInterface = (Ipv4) nouvelleValeur;
                if (monInterface != null) {
                    System.out.println("\t[ Interfaces ]");
                    System.out.printf("""
                            
                            Type : %s
                            Nom : %s
                            IP : %s
                            
                            %n""", monInterface.interfaceType(), monInterface.interfaceName(), monInterface.ip());
                }
                assert monInterface != null;
                interfaceName = monInterface.interfaceName();
                new Thread(this::getConfig).start();
            });
        } catch (Exception e) {
            DiagnosticException.afficheException(e);
        }

        voyant.setFill(RED);
        connecter.setDisable(false);
        deconnecter.setDisable(true);

    }

//    private void authenticator() throws Exception {
//        Totp totp = new Totp(CODE);
//        int code = 0;
//        Thread.sleep(1000);
//        TextAreaReponses.appendText("Entrez le code TOTP pour utiliser le client : ");
//        do {
//            button.setOnMouseClicked(_ -> TextAreaReponses.appendText(TextFieldRequette.getText()));
//            if (!totp.testCodeTOTP(parseInt(TextFieldRequette.getText()))) {
//                System.out.println("Here");
//                TextAreaReponses.appendText("Code incorrect, veuillez reessayer :");
//            }
//            else {
//                TextAreaReponses.appendText("Code bon.");
//                System.out.println("here2");
//            }
//        } while (!totp.testCodeTOTP(parseInt(TextFieldRequette.getText())));
//
//    }

    public boolean verifierTotp() throws Exception {
        Totp totp = new Totp(CODE);
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Vérification TOTP");
        dialog.setHeaderText("Saisissez votre code d'authentification.");
        dialog.setContentText("Code TOTP :");

        while (true) {
            Optional<String> saisie = dialog.showAndWait();
            if (saisie.isEmpty()) return false;
            if (totp.testCodeTOTP(parseInt(saisie.get()))) return true;

            dialog.setHeaderText("Code invalide. Réessayez.");
            dialog.getEditor().clear();
        }
    }

    private void getInterfaces() {
        try {
            ScanInterfaces.getSystemIP().forEach(ip -> {
                choiceBoxInterfaces.getItems().clear();
                choiceBoxInterfaces.getItems().addAll(ip);
            });
        } catch (SocketException e) {
            System.err.println();
        }
    }

    private void getConfig() {
        try {
            multicastDiffusion = new MulticastDiffusion(interfaceName);
            Thread.sleep(500);
            TextFieldIP.setText(multicastDiffusion.connexion.addressAsString());
            TextFieldPort.setText(String.valueOf(multicastDiffusion.connexion.portTCP()));
        } catch (Exception ex) {
            System.err.println(DiagnosticException.afficheException(ex));
        }
    }

    private void envoyer() {
        if (!enRun) return;

        String s = TextFieldRequette.getText();
        if (s.isEmpty()) return;

        if (s.equalsIgnoreCase("exit")) {
            this.deconnecter();
            return;
        }

        tcp.requette(s);
    }

    private void deconnecter() {
        if (!enRun) return;

        tcp.deconnection();
        enRun = false;

        // Nettoyage
        TextAreaReponses.clear();
        TextFieldRequette.setText("");
        choiceBoxInterfaces.getSelectionModel().clearSelection();
        multicastDiffusion = null;
    }

    private void connecter() {
        if (TextFieldIP.getText().isEmpty() || TextFieldPort.getText().isEmpty() || enRun) return;

        adresse = TextFieldIP.getText();
        port = TextFieldPort.getText();

        try {
            tcp = new TCPClient(InetAddress.getByName(adresse), parseInt(port), this); // Initialisation
        } catch (Exception e) {
            DiagnosticException.afficheException(e);
        }
        tcp.connection();

        enRun = tcp.isAlive();
    }
}