from django.conf import settings
from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail
import traceback

def send_email(recipient, subject, content, is_html):
    from_address = settings.EMAIL_HOST_USER
    message = Mail(from_email=from_address, to_emails=recipient, subject=subject, html_content=content)
    try:
        sg = SendGridAPIClient(settings.SENDGRID_API_KEY)
        response = sg.send(message)
    except Exception as e:
        print(str(e))

# Middleware that sends an email on exceptions.
# https://docs.djangoproject.com/en/2.2/topics/http/middleware/
# https://simpleisbetterthancomplex.com/tutorial/2016/07/18/how-to-create-a-custom-django-middleware.html
class EmailMiddleware(object):
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)
        return response

    def process_exception(self, request, exception):
        if (not settings.DEBUG):
            traceback_string = traceback.format_exc()
            # TODO clean up the email:
            # Formatting isn't the best. Would be better with html template and legit formatting;
            # see our password reset for an example.
            # With a template it'd be easy to include more info too (timestamp, api call, 
            # maybe a dump of request data?)
            # Could use a more clear subject line.
            # Recipients ought to be made into a configurable setting
            # (or better yet, a webmoira list we maintain).
            send_email("n8kim1@gmail.com", "Django Error Occured", traceback_string, False)
        return None
