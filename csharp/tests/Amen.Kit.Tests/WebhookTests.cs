using System.Text;
using Amen.Kit.Webhooks;
using Xunit;

public class WebhookTests
{
    // Golden vector captured from a real Amen delivery (2026-08-30).
    const string GoldenSecret = "whsec_test";
    const string GoldenTimestamp = "2026-08-30T18:53:23.885957+00:00";
    const string GoldenBody = "{\"event\":\"deal.status.changed\",\"timestamp\":\"2026-08-30T18:53:23.885957+00:00\",\"payload\":{\"number\":\"D-0000000002\",\"status\":\"paid\"}}";
    const string GoldenHex = "950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c";

    static byte[] Body => Encoding.UTF8.GetBytes(GoldenBody);

    [Fact] public void ComputeMatchesGoldenVector()
    {
        Assert.Equal(GoldenHex, WebhookSignature.Compute(GoldenSecret, GoldenTimestamp, Body));
    }

    [Fact] public void VerifyAcceptsBareAndPrefixedSignatures()
    {
        Assert.True(WebhookSignature.Verify(GoldenSecret, GoldenTimestamp, Body, GoldenHex));
        Assert.True(WebhookSignature.Verify(GoldenSecret, GoldenTimestamp, Body, "sha256=" + GoldenHex));
        Assert.True(WebhookSignature.Verify(GoldenSecret, GoldenTimestamp, Body, "sha256=" + GoldenHex.ToUpperInvariant()));
    }

    [Fact] public void VerifyRejectsTamperedBodyTimestampAndSecret()
    {
        // Tampered body.
        Assert.False(WebhookSignature.Verify(GoldenSecret, GoldenTimestamp, Encoding.UTF8.GetBytes(GoldenBody + " "), GoldenHex));
        // Wrong timestamp (part of the signed message).
        Assert.False(WebhookSignature.Verify(GoldenSecret, "2026-08-30T18:53:23.000000+00:00", Body, GoldenHex));
        // Wrong secret.
        Assert.False(WebhookSignature.Verify("whsec_other", GoldenTimestamp, Body, GoldenHex));
    }

    [Fact] public void VerifyRejectsMissingSignatureOrTimestamp()
    {
        Assert.False(WebhookSignature.Verify(GoldenSecret, GoldenTimestamp, Body, null));
        Assert.False(WebhookSignature.Verify(GoldenSecret, GoldenTimestamp, Body, ""));
        Assert.False(WebhookSignature.Verify(GoldenSecret, null, Body, GoldenHex));
        Assert.False(WebhookSignature.Verify(GoldenSecret, "", Body, GoldenHex));
    }

    static IEnumerable<KeyValuePair<string, string>> Headers(string? sig, string? ts, string? evt = "deal.status.changed")
    {
        var h = new List<KeyValuePair<string, string>>();
        if (sig is not null) h.Add(new(WebhookSignature.SignatureHeader, sig));
        if (ts is not null) h.Add(new(WebhookSignature.TimestampHeader, ts));
        if (evt is not null) h.Add(new(WebhookSignature.EventHeader, evt));
        return h;
    }

    [Fact] public async Task HandlerRejectsBadSignatureWith401()
    {
        var h = new WebhookHandler(GoldenSecret, _ => Task.CompletedTask);
        Assert.Equal(401, (await h.HandleAsync(Headers("sha256=deadbeef", GoldenTimestamp), Body)).Status);
        // Missing timestamp header also fails verification.
        Assert.Equal(401, (await h.HandleAsync(Headers("sha256=" + GoldenHex, null), Body)).Status);
    }

    [Fact] public async Task HandlerVerifiesDispatchesAndDedupesOnTimestamp()
    {
        var seen = new List<WebhookEvent>();
        var h = new WebhookHandler(GoldenSecret, e => { seen.Add(e); return Task.CompletedTask; });
        var good = Headers("sha256=" + GoldenHex, GoldenTimestamp);

        var first = await h.HandleAsync(good, Body);
        Assert.Equal(200, first.Status);
        Assert.True((bool)first.Body["ok"]);

        var second = await h.HandleAsync(good, Body);
        Assert.Equal(200, second.Status);
        Assert.Equal(true, second.Body["duplicate"]);

        Assert.Single(seen);
        Assert.Equal("deal.status.changed", seen[0].Type);
        Assert.Equal(GoldenTimestamp, seen[0].Id);
    }
}
