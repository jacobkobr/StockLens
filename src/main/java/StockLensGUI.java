import com.formdev.flatlaf.FlatDarculaLaf;
import okhttp3.*;
import org.json.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;

public class StockLensGUI {

    private static final String QUOTE_API = "https://finnhub.io/api/v1/quote?symbol=";
    private static final String NEWS_API = "https://finnhub.io/api/v1/company-news?symbol=";
    private JFrame frame;
    private JTextField apiKeyInput;
    private JTextField stockSymbolInput;
    private JTextField startDateInput;
    private JTextField endDateInput;
    private JTextPane outputArea;

    public StockLensGUI() {
        FlatDarculaLaf.install();
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

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("API Key:"));
        apiKeyInput = new JTextField(30);
        inputPanel.add(apiKeyInput);

        inputPanel.add(new JLabel("Stock Symbol:"));
        stockSymbolInput = new JTextField(30);
        inputPanel.add(stockSymbolInput);

        inputPanel.add(new JLabel("From Date (YYYY-MM-DD):"));
        startDateInput = new JTextField(10);
        inputPanel.add(startDateInput);

        inputPanel.add(new JLabel("To Date (YYYY-MM-DD):"));
        endDateInput = new JTextField(10);
        inputPanel.add(endDateInput);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton fetchQuoteBtn = new JButton("Fetch Stock Quote");
        buttonPanel.add(fetchQuoteBtn);

        JButton fetchNewsBtn = new JButton("Fetch Company News");
        buttonPanel.add(fetchNewsBtn);

        outputArea = new JTextPane();
        outputArea.setContentType("text/html");
        outputArea.setEditable(false);
        outputArea.setPreferredSize(new Dimension(300, 300));
        JScrollPane scrollPane = new JScrollPane(outputArea);

        outputArea.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    try {
                        Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        outputArea.setText("Instructions:<br>" +
                "1. Create free API key: <a href='https://finnhub.io/dashboard'>Finnhub Dashboard</a>.<br>" +
                "2. Enter your API Key and Stock Symbol of your choice.<br>" +
                "3. For news, specify the date range (YYYY-MM-DD).<br>");

        fetchQuoteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String apiKey = apiKeyInput.getText().trim();
                String stockSymbol = stockSymbolInput.getText().trim();
                if (apiKey.isEmpty() || stockSymbol.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "API Key and Stock Symbol are required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    fetchStockQuote(stockSymbol, apiKey);
                } catch (IOException ioException) {
                    outputArea.setText("Error getting stock quote.");
                    ioException.printStackTrace();
                }
            }
        });

        fetchNewsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String apiKey = apiKeyInput.getText().trim();
                String stockSymbol = stockSymbolInput.getText().trim();
                String fromDate = startDateInput.getText().trim();
                String toDate = endDateInput.getText().trim();
                if (apiKey.isEmpty() || stockSymbol.isEmpty() || fromDate.isEmpty() || toDate.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "API Key, Stock Symbol, From Date, and To Date are required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    fetchCompanyNews(stockSymbol, fromDate, toDate, apiKey);
                } catch (IOException ioException) {
                    outputArea.setText("Error getting company news.");
                    ioException.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }

    private void fetchStockQuote(String stockSymbol, String apiKey) throws IOException {
        String url = QUOTE_API + stockSymbol + "&token=" + apiKey;
        System.out.println("Calling URL: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                outputArea.setText("Error fetching stock quote. Response code: " + response.code());
                return;
            }

            String responseBody = response.body().string();
            System.out.println("Response Body: " + responseBody);

            JSONObject jsonResponse = new JSONObject(responseBody);
            if (jsonResponse.has("c")) {
                double currentPrice = jsonResponse.getDouble("c");
                double previousClose = jsonResponse.getDouble("pc");
                double change = jsonResponse.getDouble("d");
                double percentChange = jsonResponse.getDouble("dp");

                StringBuilder outputBuilder = new StringBuilder();
                outputBuilder.append("Current Stock Quote for ").append(stockSymbol).append(":<br>");
                outputBuilder.append("Current Price: ").append(currentPrice).append("<br>");
                outputBuilder.append("Previous Close: ").append(previousClose).append("<br>");
                outputBuilder.append("Change: ").append(change).append("<br>");
                outputBuilder.append("Percent Change: ").append(percentChange).append("%<br>");

                if (currentPrice > previousClose) {
                    outputBuilder.append("Recommendation: Consider a Call Option.<br>");
                } else if (currentPrice < previousClose) {
                    outputBuilder.append("Recommendation: Consider a Put Option.<br>");
                } else {
                    outputBuilder.append("Recommendation: No action needed.<br>");
                }

                outputArea.setText(outputBuilder.toString());
            } else {
                outputArea.setText("Stock quote data not available for " + stockSymbol + ".");
            }
        }
    }

    private void fetchCompanyNews(String stockSymbol, String fromDate, String toDate, String apiKey) throws IOException {
        String url = NEWS_API + stockSymbol + "&from=" + fromDate + "&to=" + toDate + "&token=" + apiKey;
        System.out.println("Calling URL: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                outputArea.setText("Error fetching company news. Response code: " + response.code());
                return;
            }
            String responseBody = response.body().string();
            System.out.println("Response Body: " + responseBody);
            JSONArray jsonResponse = new JSONArray(responseBody);
            StringBuilder newsBuilder = new StringBuilder();
            newsBuilder.append("<html><body>");
            newsBuilder.append("<h2>Company News for ").append(stockSymbol).append(":</h2>");
            for (int i = 0; i < jsonResponse.length(); i++) {
                JSONObject newsItem = jsonResponse.getJSONObject(i);
                String headline = newsItem.getString("headline");
                String publishedDate = newsItem.optString("datetime");
                String summary = newsItem.getString("summary");
                String link = newsItem.optString("url", "");

                newsBuilder.append("<p><strong>[").append(publishedDate).append("] </strong>")
                        .append("<a href='").append(link).append("'>").append(headline).append("</a><br>")
                        .append("Summary: ").append(summary).append("</p>");
            }
            newsBuilder.append("</body></html>");
            outputArea.setText(newsBuilder.toString());
        }
    }

    private boolean isValidDateFormat(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(StockLensGUI::new);
    }
}
