import java.time.LocalDate

println 'Handle the configuration context'

settings.context['context handled at'] = LocalDate.now().toString()
