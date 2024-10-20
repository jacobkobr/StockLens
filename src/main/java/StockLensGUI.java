import com.formdev.flatlaf.FlatDarculaLaf;
import okhttp3.*;
import org.json.*;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StockLensGUI {

    private static final String QUOTE_API = "https://finnhub.io/api/v1/quote?symbol=";
    private static final String NEWS_API = "https://finnhub.io/api/v1/company-news?symbol=";
    private JFrame frame;
    private JTextField apiKeyInput;
    private JTextField stockSymbolInput;
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JTextPane outputArea;

    public StockLensGUI() {
        FlatDarculaLaf.install();  // Set the theme
        structure();
    }

    private void structure() {
        frame = new JFrame("Stock Info Fetcher - StockLens");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        ImageIcon icon = new ImageIcon(getClass().getResource("/stocklens.png"));
        frame.setIconImage(icon.getImage());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("API Key:"));
        apiKeyInput = new JTextField(30);
        inputPanel.add(apiKeyInput);

        inputPanel.add(new JLabel("Stock Symbol:"));
        stockSymbolInput = new JTextField(30);
        inputPanel.add(stockSymbolInput);

        inputPanel.add(new JLabel("From Date:"));
        startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        inputPanel.add(startDateChooser);

        inputPanel.add(new JLabel("To Date:"));
        endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        inputPanel.add(endDateChooser);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton fetchQuoteBtn = new JButton("Fetch Stock Quote");
        buttonPanel.add(fetchQuoteBtn);

        JButton fetchNewsBtn = new JButton("Fetch Company News");
        buttonPanel.add(fetchNewsBtn);

        outputArea = new JTextPane();
        outputArea.setContentType("text/html");
        outputArea.setEditable(false);
        outputArea.setPreferredSize(new Dimension(500, 200));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));


        outputArea.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);
        outputArea.setText(
                "Instructions:<br>" +
                        "1. Create free API key: <a href='https://finnhub.io/dashboard'>Finnhub Dashboard</a>.<br>" +
                        "2. Enter your API Key and Stock Symbol of your choice.<br>" +
                        "3. For news, specify the date range using the calendar pop-up.<br>"
        );
        fetchQuoteBtn.addActionListener(e -> fetchStockQuote());
        fetchNewsBtn.addActionListener(e -> fetchCompanyNews());
        frame.setVisible(true);
    }

    private void fetchStockQuote() {
        String apiKey = apiKeyInput.getText().trim();
        String stockSymbol = stockSymbolInput.getText().trim();
        if (apiKey.isEmpty() || stockSymbol.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "API Key and Stock Symbol are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String url = QUOTE_API + stockSymbol + "&token=" + apiKey;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                outputArea.setText("Error fetching stock quote. Response code: " + response.code());
                return;
            }

            JSONObject jsonResponse = new JSONObject(response.body().string());
            String output = "Current Stock Quote for " + stockSymbol + ":<br>" +
                    "Current Price: " + jsonResponse.getDouble("c") + "<br>" +
                    "Previous Close: " + jsonResponse.getDouble("pc") + "<br>" +
                    "Change: " + jsonResponse.getDouble("d") + "<br>" +
                    "Percent Change: " + jsonResponse.getDouble("dp") + "%<br>";
            outputArea.setText(output);
        } catch (IOException e) {
            outputArea.setText("Error getting stock quote.");
            e.printStackTrace();
        }
    }

    private void fetchCompanyNews() {
        String apiKey = apiKeyInput.getText().trim();
        String stockSymbol = stockSymbolInput.getText().trim();
        Date fromDate = startDateChooser.getDate();
        Date toDate = endDateChooser.getDate();

        if (apiKey.isEmpty() || stockSymbol.isEmpty() || fromDate == null || toDate == null) {
            JOptionPane.showMessageDialog(frame, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String fromDateString = new SimpleDateFormat("yyyy-MM-dd").format(fromDate);
        String toDateString = new SimpleDateFormat("yyyy-MM-dd").format(toDate);

        String url = NEWS_API + stockSymbol + "&from=" + fromDateString + "&to=" + toDateString + "&token=" + apiKey;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                outputArea.setText("Error fetching company news. Response code: " + response.code());
                return;
            }

            JSONArray newsArray = new JSONArray(response.body().string());
            StringBuilder newsBuilder = new StringBuilder("<html><body>");

            for (int i = 0; i < newsArray.length(); i++) {
                JSONObject newsItem = newsArray.getJSONObject(i);
                newsBuilder.append("<p><strong>")
                        .append(newsItem.optString("datetime")).append("</strong>: ")
                        .append("<a href='").append(newsItem.optString("url")).append("'>")
                        .append(newsItem.getString("headline")).append("</a><br>")
                        .append(newsItem.getString("summary")).append("</p>");
            }

            newsBuilder.append("</body></html>");
            outputArea.setText(newsBuilder.toString());
        } catch (IOException e) {
            outputArea.setText("Error getting company news.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {SwingUtilities.invokeLater(StockLensGUI::new);
    }
}
