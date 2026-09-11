package io.github.querytemplate;

/**
 * {@link FillVerifier} that uses a {@link FillVerifierDelegate}. Makes it
 * possible to build a {@link FillVerifier} with inline code. Example:
 *
 * <pre>
 * FillVerifier fv = new FillVerifierByDelegate((filter, filterPrp) -&gt; {
 *     try {
 *         return PropertyUtils.getProperty(filter, filterPrp) != null;
 *     } catch (Exception e) {
 *         throw new QueryTemplateException("filterPrp: '" + filterPrp + "'", e);
 *     }
 * });
 * </pre>
 */
public class FillVerifierByDelegate implements FillVerifier {

    private final FillVerifierDelegate fillVerifierDelegate;

    /**
     * @param fillVerifierDelegate the delegate used to verify.
     */
    public FillVerifierByDelegate(FillVerifierDelegate fillVerifierDelegate) {
        this.fillVerifierDelegate = fillVerifierDelegate;
    }

    @Override
    public boolean isFilled(Object filter, String filterPrp) {
        return this.fillVerifierDelegate.isFill(filter, filterPrp);
    }
}
