<!DOCTYPE html>
<html data-layout-decorator="layouts/default-layout">
    <head>
        <meta charset="UTF-8" data-th-remove="all" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" data-th-remove="all" />
        <meta name="viewport" content="width=device-width, initial-scale=1"
          data-th-remove="all" />
        <meta name="description"
          content="Spring Roo"
          data-th-remove="all" />
        <meta name="author"
          content="Spring Roo"
          data-th-remove="all" />
        <link data-th-remove="all" rel="icon" href="../static/public/img/favicon.ico" />

        <title data-th-text="${r"#{"}label_error${r"}"}">Error</title>

        <!-- Bootstrap -->
    <link rel="stylesheet" type="text/css"
      href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.css"
      data-th-remove="all"></link>

    <!-- MSSSI CSS -->
    <link rel="stylesheet" type="text/css"
      href="../static/public/css/sanidad-internet.css" data-th-remove="all" />

    <!-- HTML5 shim y Respond.js para soporte de elementos HTML5 en IE8 y media queries -->
    <!--[if lt IE 9]>
       <script src="/public/js/html5shiv.min.js"></script>
        <script src="/public/js/respond.min.js"></script>
    <![endif]-->

    </head>
<body>
  <header>
    <h1 data-th-text="${r"#{"}label_error_page${r"}"}">Error Page</h1>
  </header>

  <section data-layout-fragment="content">

    <div class="alert alert-danger fade in" role="alert">
      <h4>¡Error!</h4>
      <div>
        <span data-th-text="${r"#{info_error}"}">An error occurred</span>
        (type=<span data-th-text="${r"${error}"}">Bad</span>, status=<span data-th-text="${r"${status}"}">500</span>).
      </div>
      <hr/>
      <div data-text="${r"${message}"}"></div>
      <hr/>
    </div>

  </section>
  <footer> &copy; Powered By Spring Roo </footer>
</body>
</html>