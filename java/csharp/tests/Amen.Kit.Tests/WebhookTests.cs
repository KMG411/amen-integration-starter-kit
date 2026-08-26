using System.Text;
using Amen.Kit.Webhooks;
using Xunit;

public class WebhookTests
{
    const string Secret = "unit-test-secret";
    [Fact] public void SignatureRoundtrip()
    {
        var body = Encoding.UTF8.GetBytes("{\"id\":\"e1\",\"event\":\"deal.paid\"}"); var sig = WebhookSignature.Compute(Secret, body);
        Assert.True(WebhookSignature.Verify(Secret, body, sig)); Assert.True(WebhookSignature.Verify(Secret, body, "sha256=" + sig));
        Assert.False(WebhookSignature.Verify(Secret, Encoding.UTF8.GetBytes("{\"id\":\"e1\",\"event\":\"deal.paid\"} "), sig)); Assert.False(WebhookSignature.Verify(Secret, body, null));
    }
    [Fact] public async Task HandlerVerifiesAndDedupes()
    {
        var seen = new List<string>(); var h = new WebhookHandler(Secret, e => { seen.Add(e.Id); return Task.CompletedTask; });
        var body = Encoding.UTF8.GetBytes("{\"id\":\"e1\",\"event\":\"deal.paid\"}"); var good = new[] { new KeyValuePair<string, string>("X-Signature", WebhookSignature.Compute(Secret, body)) };
        Assert.Equal(401, (await h.HandleAsync([new("X-Signature", "bad")], body)).Status);
        Assert.Equal(200, (await h.HandleAsync(good, body)).Status);
        Assert.Equal(true, (await h.HandleAsync(good, body)).Body["duplicate"]);
        Assert.Equal(["e1"], seen);
    }
}
