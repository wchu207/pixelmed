path=.;.\lib\additional;%path%

rem see "https://www.dclunie.com/pixelmed/software/javadoc/com/pixelmed/validate/DicomSRValidator.html"

rem [-describe] [-donotcheckcodemeaning] [-donotmatchcase] [-donotcheckdeprecatedcodingscheme] [-checkambiguoustemplate] [-checkcontentitemorder] [-checktemplateid] [-donotcheckcontentitemsnotintemplate]

java -Xmx1g -XX:-UseGCOverheadLimit -Djavax.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl -cp ".\pixelmed.jar;.\lib\additional\slf4j-api-1.7.13.jar;.\lib\additional\slf4j-simple-1.7.13.jar;.\lib\additional\excalibur-bzip2-1.0.jar;.\lib\additional\commons-codec-1.3.jar;.\lib\additional\saxon-he-12.5.jar;.\lib\additional\xmlresolver-5.2.2.jar" com.pixelmed.validate.DicomSRValidator -checkcontentitemorder -checktemplateid %1

pause
